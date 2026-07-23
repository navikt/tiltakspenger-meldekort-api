package no.nav.tiltakspenger.meldekort.journalføring.infra

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.harStatus
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.feil.bodySomJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.NavHeadere
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.retry.Retry
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.meldekort.journalføring.DokarkivClient
import no.nav.tiltakspenger.meldekort.journalføring.JournalpostId
import no.nav.tiltakspenger.meldekort.journalføring.PdfOgJson
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

const val INDIVIDSTONAD = "IND"
const val DOKARKIV_PATH = "rest/journalpostapi/v1/journalpost"

/**
 * Dokarkiv (tidligere Joark) er en tjeneste som brukes for å opprette journalposter i Nav.
 *
 * Kildekode: https://github.com/navikt/dokarkiv
 * Dokumentasjon: https://confluence.adeo.no/display/BOA/dokarkiv og https://confluence.adeo.no/display/BOA/opprettJournalpost
 * API-spec: https://dokarkiv.dev.intern.nav.no/swagger-ui/index.html
 * Slack: #team-dokumentløsninger (https://nav-it.slack.com/archives/C6W9E5GPJ)
 * Teamkatalog: https://teamkatalogen.nav.no/team/f3388fcd-898e-40da-8d02-0bf1e3a79120
 *
 * `409 Conflict` er et domeneutfall (meldekortet er allerede journalført, dedup på eksternReferanseId) med samme bodyform som `201`: den utledes fra feiltypen med [harStatus]/[bodySomJson] i stedet for å stå i statusregelen.
 * Dedupen er også grunnen til at [Retry.Fast.retryIkkeIdempotente] er trygt her: et nytt forsøk etter et uvisst utfall gir i verste fall en `409` som behandles som suksess.
 *
 * Klienten logger ikke feil selv; feillogging skjer én gang i [no.nav.tiltakspenger.meldekort.journalføring.JournalførMeldekortService].
 * Unntaket er én error-linje når dokarkiv oppretter journalposten uten å ferdigstille den — kallet er da en suksess (ingen Left å logge hos kalleren), men tilstanden krever manuell oppfølging.
 *
 * @param transport Nettverks-sømmen til [HttpKlient]; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen kjører.
 */
class DokarkivClientImpl(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 5.seconds,
    timeout: Duration = 30.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : DokarkivClient {
    private val log = KotlinLogging.logger {}

    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
            // Paritet med den gamle ktor-klienten (`httpClientWithRetry`): fire forsøk totalt med konstant 100 ms delay.
            retry = Retry.Fast(maksForsøk = 4, delay = 100.milliseconds, retryIkkeIdempotente = true),
        ),
        transport = transport,
    )

    private val opprettJournalpostUri = URI.create("$baseUrl/$DOKARKIV_PATH")

    override suspend fun journalførMeldekort(
        meldekort: BrukersMeldekort,
        pdfOgJson: PdfOgJson,
        callId: CorrelationId,
        clock: Clock,
        pdfgenrs: Boolean,
    ): Either<HttpKlientError, JournalpostId> {
        val request = meldekort.toJournalpostDokument(pdfOgJson = pdfOgJson, clock = clock, pdfgenrs = pdfgenrs)

        return httpKlient.postJson<DokarkivResponse>(
            uri = URI.create("$opprettJournalpostUri?forsoekFerdigstill=${request.kanFerdigstilleAutomatisk()}"),
            body = SerialisertJson(objectMapper.writeValueAsString(request)),
            headere = listOf(
                NavHeadere.xCorrelationId(callId.value),
                // Dokarkiv bruker «Nav-Callid»-varianten (uten bindestrek før Id); navCallId ville gitt det andre headernavnet «Nav-Call-Id».
                NavHeadere.navCallid(callId.value),
            ),
            godta = Statusregel.Eksakt(201),
        ).fold(
            ifRight = { respons ->
                respons.body.verifiserFerdigstilt(request.kanFerdigstilleAutomatisk()).right()
            },
            ifLeft = { feil ->
                when {
                    // Allerede journalført (dedup): samme bodyform som 201, utledes fra feiltypen.
                    feil.harStatus(409) && feil is HttpKlientError.UventetStatus ->
                        feil.bodySomJson<DokarkivResponse>()
                            .map { it.journalpostId }

                    else -> feil.left()
                }
            },
        )
    }

    private fun DokarkivResponse.verifiserFerdigstilt(kanFerdigstilleAutomatisk: Boolean): JournalpostId {
        if (kanFerdigstilleAutomatisk && !journalpostferdigstilt) {
            // Suksess uten Left, så kalleren har ingen feil å logge — dette driftssignalet (manuell ferdigstilling i Gosys) logges derfor her.
            log.error { "Dokarkiv opprettet journalpost $journalpostId uten å ferdigstille den. Se sikkerlogg for detaljer." }
            Sikkerlogg.error { "Dokarkiv opprettet journalpost $journalpostId uten å ferdigstille den." }
        }
        return journalpostId
    }

    data class DokarkivResponse(
        val journalpostId: JournalpostId,
        val journalpostferdigstilt: Boolean,
    )
}
