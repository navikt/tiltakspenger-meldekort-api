package no.nav.tiltakspenger.meldekort.clients.saksbehandling

import arrow.core.Either
import arrow.core.left
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.logging.sikkerlogg
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
    connectTimeout: Duration = 1.seconds,
    private val timeout: Duration = 1.seconds,
) : SaksbehandlingClient {

    // TODO Kew: Legg inn riktig url når den er laget i saksbehandling-api
    private val saksbehandlingApiMeldekortUri = URI.create("$baseUrl/meldekort")

    private val client = java.net.http.HttpClient.newBuilder()
        .connectTimeout(connectTimeout.toJavaDuration())
        .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
        .build()

    private val logger = KotlinLogging.logger {}

    override suspend fun sendMeldekort(meldekort: Meldekort, correlationId: CorrelationId): Either<SaksbehandlingApiError, Unit> {
        val jsonPayload = serialize(meldekort.toSaksbehandlingMeldekortDTO())

        return Either.catch {
            logger.info { "Sender kall til saksbehandling med id ${meldekort.id}" }

            val request = createRequest(token = getToken(), jsonPayload = jsonPayload)
            val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()

            val responseBody = httpResponse.body()
            val status = httpResponse.statusCode()
            if (status != 200) {
                logger.error { "Feil ved kall til tiltakspenger-saksbehandling-api. MeldekortId: ${meldekort.id} Se sikkerlogg for detaljer." }
                sikkerlogg.error { "Feil ved kall til tiltakspenger-saksbehandling-api. MeldekortId: ${meldekort.id}. JsonResponse: $responseBody jsonPayload: $jsonPayload" }
                return SaksbehandlingApiError.left()
            }
            Unit
        }.mapLeft {
            // Either.catch slipper igjennom CancellationException som er ønskelig.
            logger.error(it) { "Feil ved kall til tiltakspenger-saksbehandling-api.. Se sikkerlogg for detaljer." }
            sikkerlogg.error(it) { "Feil ved kall til tiltakspenger-saksbehandling-api.. jsonPayload: $jsonPayload, uri: $saksbehandlingApiMeldekortUri" }
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
