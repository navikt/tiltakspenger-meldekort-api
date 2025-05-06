package no.nav.tiltakspenger.meldekort.clients.pdfgen

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.meldekort.clients.httpClientWithRetry
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.journalføring.KunneIkkeGenererePdf
import no.nav.tiltakspenger.meldekort.domene.journalføring.PdfA
import no.nav.tiltakspenger.meldekort.domene.journalføring.PdfOgJson
import toBrevMeldekortDTO
import java.util.UUID

const val PDFGEN_PATH = "api/v1/genpdf/tpts"

/**
 * Konverterer domene til JSON som sendes til https://github.com/navikt/tiltakspenger-pdfgen for å generere PDF.
 */
class PdfgenClient(
    private val baseUrl: String = Configuration.pdfgenUrl,
    private val client: HttpClient = httpClientWithRetry(),
) {
    private val log = KotlinLogging.logger {}

    suspend fun genererPdf(
        meldekort: Meldekort,
        errorContext: String = "SakId: ${meldekort.sakId}, saksnummer: ${meldekort.meldeperiode.saksnummer} meldekortId: ${meldekort.id}",
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            uri = "$baseUrl/$PDFGEN_PATH/meldekort",
            jsonPayload = { meldekort.toBrevMeldekortDTO() },
            errorContext = errorContext,
        )
    }

    private suspend fun pdfgenRequest(
        uri: String,
        jsonPayload: suspend () -> String,
        errorContext: String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return withContext(Dispatchers.IO) {
            Either.catch {
                val httpResponse = client.post(uri) {
                    accept(ContentType.Application.Json)
                    header("X-Correlation-ID", UUID.randomUUID())
                    contentType(ContentType.Application.Json)
                    setBody(jsonPayload())
                }
                val status = httpResponse.status
                val jsonResponse = httpResponse.body<String>()
                if (status != HttpStatusCode.OK) {
                    log.error { "Feil ved kall til pdfgen. $errorContext. Status: $status. uri: $uri. Se sikkerlogg for detaljer." }
                    sikkerlogg.error { "Feil ved kall til pdfgen. $errorContext. uri: $uri. jsonResponse: $jsonResponse. jsonPayload: $jsonPayload." }
                    return@withContext KunneIkkeGenererePdf.left()
                }
                PdfOgJson(PdfA(httpResponse.body()), jsonPayload())
            }.mapLeft {
                // Either.catch slipper igjennom CancellationException som er ønskelig.
                log.error(it) { "Feil ved kall til pdfgen. $errorContext. Se sikkerlogg for detaljer." }
                sikkerlogg.error(it) { "Feil ved kall til pdfgen. $errorContext. jsonPayload: $jsonPayload, uri: $uri" }
                KunneIkkeGenererePdf
            }
        }
    }
}
