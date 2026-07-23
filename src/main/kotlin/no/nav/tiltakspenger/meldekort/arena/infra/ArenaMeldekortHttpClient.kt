package no.nav.tiltakspenger.meldekort.arena.infra

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Header
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.retry.Retry
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortClient
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortOversikt
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Klient for å hente Arena-meldekort for en bruker fra meldekortservice.
 *
 * Kildekode: https://github.com/navikt/meldekortservice
 * Dokumentasjon: https://confluence.adeo.no/display/TMP/Meldekort-api
 * API-spec: https://meldekortservice-q2.dev-fss-pub.nais.io/meldekortservice/internal/apidocs/index.html (samme sti på q1- og prod-ingressen)
 * Slack: #team-meldeplikt
 * Teamkatalog: https://teamkatalogen.nav.no/team/f0752a93-3728-4f63-bd56-053c1fe99a6e
 *
 * Timeouten på 60 sekunder er arvet fra den gamle ktor-klienten; meldekortservice ligger i FSS og kan være treg.
 * Retryen replikerer den gamle ktor-klienten (`httpClientWithRetry`): fire forsøk totalt med konstant 100 ms delay.
 *
 * @param transport Nettverks-sømmen til [HttpKlient]; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen kjører.
 */
class ArenaMeldekortHttpClient(
    private val baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 5.seconds,
    timeout: Duration = 60.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : ArenaMeldekortClient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
            retry = Retry.Fast(maksForsøk = 4, delay = 100.milliseconds, retryIkkeIdempotente = true),
        ),
        transport = transport,
    )

    /** `Right(null)` betyr at brukeren ikke har meldekort i Arena (meldekortservice svarer `204`). */
    override suspend fun hentMeldekort(fnr: Fnr): Either<HttpKlientError, ArenaMeldekortOversikt?> {
        return httpKlient.getJsonEllerNull<ArenaMeldekortResponse>(
            uri = URI.create("$baseUrl/meldekortservice/api/v2/meldekort"),
            nullVedStatus = setOf(204),
            headere = listOf(identHeader(fnr)),
        ).map { it.body?.tilArenaMeldekortOversikt() }
    }

    /**
     * meldekortservice svarer med feilstatus (typisk `503`) på historikk-oppslag når brukeren ikke finnes.
     * Vi kaller aldri denne uten å ha kalt [hentMeldekort] først, så alle feilstatuser tolkes som «brukeren finnes ikke» og gir `Right(null)` — kun transportfeil o.l. gir Left.
     */
    override suspend fun hentHistoriskeMeldekort(
        fnr: Fnr,
        antallMeldeperioder: Int,
    ): Either<HttpKlientError, ArenaMeldekortOversikt?> {
        return httpKlient.getJsonEllerNull<ArenaMeldekortResponse>(
            uri = URI.create("$baseUrl/meldekortservice/api/v2/historiskemeldekort?antallMeldeperioder=$antallMeldeperioder"),
            nullVedStatus = setOf(204),
            headere = listOf(identHeader(fnr)),
        ).fold(
            ifRight = { it.body?.tilArenaMeldekortOversikt().right() },
            ifLeft = { feil ->
                if (feil is HttpKlientError.UventetStatus) null.right() else feil.left()
            },
        )
    }

    /** Fnr sendes som header og markeres sensitiv slik at den maskeres i rå request-strengen (sikkerlogg). */
    private fun identHeader(fnr: Fnr) = Header("ident", fnr.verdi, sensitiv = true)
}
