package no.nav.tiltakspenger.meldekort.clients.saksbehandling

import arrow.core.Either
import arrow.core.left
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class SaksbehandlingClientImpl(
    baseUrl: String,
    val getToken: suspend () -> AccessToken,
    connectTimeout: Duration = 10.seconds,
    private val timeout: Duration = 10.seconds,
) : SaksbehandlingClient {

    private val saksbehandlingApiMeldekortUri = URI.create("$baseUrl/meldekort/motta")

    private val client = java.net.http.HttpClient.newBuilder()
        .connectTimeout(connectTimeout.toJavaDuration())
        .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
        .build()

    private val logger = KotlinLogging.logger {}

    override suspend fun sendMeldekort(meldekort: Meldekort): Either<SaksbehandlingApiError, Unit> {
        val jsonPayload = serialize(meldekort.toSaksbehandlingMeldekortDTO())

        return Either.catch {
            logger.info { "Sender kall til saksbehandling med id ${meldekort.id}" }

            val request = createRequest(token = getToken(), jsonPayload = jsonPayload)
            val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()

            val responseBody = httpResponse.body()
            val status = httpResponse.statusCode()
            if (status != 200) {
                val melding = "Feil ved kall til tiltakspenger-saksbehandling-api - Status $status - Id: ${meldekort.id}"
                logger.error { "$melding - Se sikkerlogg for detaljer." }
                Sikkerlogg.error { "$melding - Response: $responseBody - Payload: $jsonPayload" }
                return SaksbehandlingApiError.left()
            }
            Unit
        }.mapLeft {
            // Either.catch slipper igjennom CancellationException som er ønskelig.
            logger.error(it) { "Feil ved kall til tiltakspenger-saksbehandling-api.. Se sikkerlogg for detaljer." }
            Sikkerlogg.error(it) { "Feil ved kall til tiltakspenger-saksbehandling-api.. jsonPayload: $jsonPayload, uri: $saksbehandlingApiMeldekortUri" }
            SaksbehandlingApiError
        }
    }

    private fun createRequest(
        token: AccessToken,
        jsonPayload: String,
    ): HttpRequest? {
        return HttpRequest.newBuilder()
            .uri(saksbehandlingApiMeldekortUri)
            .timeout(timeout.toJavaDuration())
            .header("Authorization", "Bearer ${token.token}")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
    }
}
