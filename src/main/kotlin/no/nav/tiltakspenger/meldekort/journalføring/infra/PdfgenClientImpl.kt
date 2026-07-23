package no.nav.tiltakspenger.meldekort.journalføring.infra

import arrow.core.Either
import arrow.core.flatMap
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import kotlin.time.measureTimedValue

const val PDFGEN_PATH = "api/v1/genpdf/tpts"

/**
 * Konverterer domene til JSON som sendes til pdfgen og pdfgenrs for å generere PDF.
 *
 * Kildekode: https://github.com/navikt/tiltakspenger-pdfgen og https://github.com/navikt/tiltakspenger-pdfgenrs
 * Dokumentasjon: README-ene i kildekode-repoene
 * API-spec: -
 * Slack: #tiltakspenger-værsågod (eget team)
 * Teamkatalog: https://teamkatalogen.nav.no/team/15bca3d2-2584-4167-85ba-faab1f1cfb53
 *
 * pdfgen har ingen autentisering, derfor ingen auth i klienten.
 * Retryen replikerer den gamle ktor-klienten (`httpClientWithRetry`): fire forsøk totalt med konstant 100 ms delay.
 *
 * @param transport Nettverks-sømmen til [HttpKlient]; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen kjører.
 */
class PdfgenClientImpl(
    private val baseUrl: String = Configuration.pdfgenUrl,
    private val pdfgenrsBaseUrl: String = Configuration.pdfgenrsUrl,
    private val isLocalOrDev: Boolean,
    clock: Clock,
    connectTimeout: Duration = 5.seconds,
    timeout: Duration = 10.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : PdfgenClient {
    private val log = KotlinLogging.logger {}

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
    ): Either<HttpKlientError, Pair<PdfOgJson, PdfOgJson?>> {
        return generer(meldekort, path = "meldekort")
    }

    override suspend fun genererKorrigertMeldekortPdf(
        meldekort: BrukersMeldekort,
    ): Either<HttpKlientError, Pair<PdfOgJson, PdfOgJson?>> {
        return generer(meldekort, path = "meldekort-korrigert")
    }

    private suspend fun generer(
        meldekort: BrukersMeldekort,
        path: String,
    ): Either<HttpKlientError, Pair<PdfOgJson, PdfOgJson?>> {
        val språksuffiks = if (meldekort.locale == "en") "-en" else ""
        val pdfgenUri = URI.create("$baseUrl/$PDFGEN_PATH/$path$språksuffiks")
        val pdfgenrsUri = URI.create("$pdfgenrsBaseUrl/$PDFGEN_PATH/$path$språksuffiks")
        val jsonPayload = meldekort.toDTO()

        return if (isLocalOrDev) {
            runParallel(jsonPayload = jsonPayload, pdfgenUri = pdfgenUri, pdfgenrsUri = pdfgenrsUri)
        } else {
            pdfgenRequest(uri = pdfgenUri, jsonPayload = jsonPayload).map { it to null }
        }
    }

    private suspend fun runParallel(
        jsonPayload: String,
        pdfgenUri: URI,
        pdfgenrsUri: URI,
    ): Either<HttpKlientError, Pair<PdfOgJson, PdfOgJson?>> {
        return coroutineScope {
            val pdfgenDeferred = async {
                measureTimedValue { pdfgenRequest(pdfgenUri, jsonPayload) }
            }
            val pdfgenrsDeferred = async {
                measureTimedValue { pdfgenRequest(pdfgenrsUri, jsonPayload) }
            }

            val (pdfgenResult, pdfgenDuration) = pdfgenDeferred.await()
            val (pdfgenrsResult, pdfgenrsDuration) = pdfgenrsDeferred.await()

            // Midlertidig sammenlikningslogg mens pdfgenrs verifiseres mot pdfgen (kjører kun lokalt/dev); fjernes sammen med dobbeltkjøringen.
            log.info { "pdfgen brukte $pdfgenDuration, pdfgenrs brukte $pdfgenrsDuration" }

            pdfgenResult.flatMap { pdfgen ->
                pdfgenrsResult.map { pdfgenrs ->
                    Pair(pdfgen, pdfgenrs)
                }
            }
        }
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
