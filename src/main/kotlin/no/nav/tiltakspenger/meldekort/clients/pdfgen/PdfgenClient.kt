package no.nav.tiltakspenger.meldekort.clients.pdfgen

import arrow.core.Either
import arrow.core.left
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.journalføring.KunneIkkeGenererePdf
import no.nav.tiltakspenger.meldekort.domene.journalføring.PdfA
import no.nav.tiltakspenger.meldekort.domene.journalføring.PdfOgJson
import toBrevMeldekortDTO
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal const val PDFGEN_PATH = "api/v1/genpdf/tpts"

/**
 * Konverterer domene til JSON som sendes til https://github.com/navikt/tiltakspenger-pdfgen for å generere PDF.
 */
class PdfgenClient(
    private val baseUrl: String,
    connectTimeout: Duration = 1.seconds,
    private val timeout: Duration = 6.seconds,
) {
    private val log = KotlinLogging.logger {}
    private val client =
        HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout.toJavaDuration())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build()

    suspend fun genererPdf(
        meldekort: Meldekort,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return pdfgenRequest(
            uri = "$baseUrl/$PDFGEN_PATH/meldekort",
            jsonPayload = { meldekort.toBrevMeldekortDTO() },
            errorContext = "SakId: ${meldekort.sakId}, saksnummer: ${meldekort.meldeperiode.saksnummer} meldekortId: ${meldekort.id}",
        )
    }

    private suspend fun pdfgenRequest(
        uri: String,
        jsonPayload: suspend () -> String,
        errorContext: String,
    ): Either<KunneIkkeGenererePdf, PdfOgJson> {
        return withContext(Dispatchers.IO) {
            Either.catch {
                val request = createPdfgenRequest(uri, jsonPayload())
                val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).await()
                val jsonResponse = httpResponse.body()
                val status = httpResponse.statusCode()
                if (status != 200) {
                    log.error { "Feil ved kall til pdfgen. $errorContext. Status: $status. uri: $uri. Se sikkerlogg for detaljer." }
                    sikkerlogg.error { "Feil ved kall til pdfgen. $errorContext. uri: $uri. jsonResponse: $jsonResponse. jsonPayload: $jsonPayload." }
                    return@withContext KunneIkkeGenererePdf.left()
                }
                PdfOgJson(PdfA(jsonResponse), jsonPayload())
            }.mapLeft {
                // Either.catch slipper igjennom CancellationException som er ønskelig.
                log.error(it) { "Feil ved kall til pdfgen. $errorContext. Se sikkerlogg for detaljer." }
                sikkerlogg.error(it) { "Feil ved kall til pdfgen. $errorContext. jsonPayload: $jsonPayload, uri: $uri" }
                KunneIkkeGenererePdf
            }
        }
    }

    private fun createPdfgenRequest(
        uri: String,
        jsonPayload: String,
    ): HttpRequest? {
        return HttpRequest
            .newBuilder()
            .uri(URI.create(uri))
            .timeout(timeout.toJavaDuration())
            .header("Accept", "application/pdf")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
    }
}
