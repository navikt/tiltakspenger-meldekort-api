package no.nav.tiltakspenger.meldekort.journalføring.infra

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.NavHeadere
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.retry.Retry
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.meldekort.infra.Configuration
import no.nav.tiltakspenger.meldekort.journalføring.PdfA
import no.nav.tiltakspenger.meldekort.journalføring.PdfOgJson
import no.nav.tiltakspenger.meldekort.journalføring.PdfgenClient
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import java.net.URI
import java.time.Clock
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

const val PDFGEN_PATH = "api/v1/genpdf/tpts"

/**
 * Konverterer domene til JSON som sendes til pdfgenrs for å generere PDF.
 *
 * Kildekode: https://github.com/navikt/tiltakspenger-pdfgenrs
 * Dokumentasjon: README-en i kildekode-repoet
 * API-spec: -
 * Slack: #tiltakspenger-værsågod (eget team)
 * Teamkatalog: https://teamkatalogen.nav.no/team/15bca3d2-2584-4167-85ba-faab1f1cfb53
 *
 * pdfgenrs har ingen autentisering, derfor ingen auth i klienten.
 * Retryen replikerer den gamle ktor-klienten (`httpClientWithRetry`): fire forsøk totalt med konstant 100 ms delay.
 *
 * @param transport Nettverks-sømmen til [HttpKlient]; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen kjører.
 */
class PdfgenClientImpl(
    private val baseUrl: String = Configuration.pdfgenrsUrl,
    clock: Clock,
    connectTimeout: Duration = 5.seconds,
    timeout: Duration = 10.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : PdfgenClient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            retry = Retry.Fast(maksForsøk = 4, delay = 100.milliseconds, retryIkkeIdempotente = true),
        ),
        transport = transport,
    )

    override suspend fun genererMeldekortPdf(
        meldekort: BrukersMeldekort,
    ): Either<HttpKlientError, PdfOgJson> {
        return generer(meldekort, path = "meldekort")
    }

    override suspend fun genererKorrigertMeldekortPdf(
        meldekort: BrukersMeldekort,
    ): Either<HttpKlientError, PdfOgJson> {
        return generer(meldekort, path = "meldekort-korrigert")
    }

    private suspend fun generer(
        meldekort: BrukersMeldekort,
        path: String,
    ): Either<HttpKlientError, PdfOgJson> {
        val språksuffiks = if (meldekort.locale == "en") "-en" else ""
        val uri = URI.create("$baseUrl/$PDFGEN_PATH/$path$språksuffiks")
        val jsonPayload = meldekort.toDTO()

        return pdfgenRequest(uri = uri, jsonPayload = jsonPayload)
    }

    private suspend fun pdfgenRequest(
        uri: URI,
        jsonPayload: String,
    ): Either<HttpKlientError, PdfOgJson> {
        return httpKlient.postJsonMotPdf(
            uri = uri,
            body = SerialisertJson(jsonPayload),
            headere = listOf(NavHeadere.xCorrelationId(UUID.randomUUID().toString())),
            godta = Statusregel.Eksakt(200),
        ).map { PdfOgJson(PdfA(it.body), jsonPayload) }
    }
}
