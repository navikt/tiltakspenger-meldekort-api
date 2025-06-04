package no.nav.tiltakspenger.meldekort.clients.saksbehandling

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class SaksbehandlingClient(
    baseUrl: String,
    val getToken: suspend () -> AccessToken,
    connectTimeout: Duration = 10.seconds,
    private val timeout: Duration = 10.seconds,
) {

    private val saksbehandlingApiMeldekortUri = URI.create("$baseUrl/meldekort/motta")
    private val saksbehandlingApiHarSoknadUnderBehandlingUri = URI.create("$baseUrl/har-soknad")

    private val client = java.net.http.HttpClient.newBuilder()
        .connectTimeout(connectTimeout.toJavaDuration())
        .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
        .build()

    private val logger = KotlinLogging.logger {}

    suspend fun sendMeldekort(meldekort: Meldekort): Either<SaksbehandlingApiError, Unit> {
        val jsonPayload = serialize(meldekort.toSaksbehandlingMeldekortDTO())

        return Either.catch {
            logger.info { "Sender kall til saksbehandling med id ${meldekort.id}" }

            val request = createRequest(uri = saksbehandlingApiMeldekortUri, token = getToken(), jsonPayload = jsonPayload)
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

    suspend fun harSoknadUnderBehandling(fnr: Fnr): Either<SaksbehandlingApiError, Boolean> {
        val jsonPayload = serialize(FnrDTO(fnr.verdi))

        return Either.catch {
            logger.info { "Sjekker om bruker har søknader under behandling" }

            val request = createRequest(
                uri = saksbehandlingApiHarSoknadUnderBehandlingUri,
                token = getToken(),
                jsonPayload = jsonPayload,
            )
            val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()

            val responseBody = httpResponse.body()
            val status = httpResponse.statusCode()
            if (status != 200) {
                val melding = "Feil ved kall til tiltakspenger-saksbehandling-api for å sjekke søknader under behandling - Status $status"
                logger.error { "$melding - Se sikkerlogg for detaljer." }
                Sikkerlogg.error { "$melding - Response: $responseBody - Payload: $jsonPayload" }
                return SaksbehandlingApiError.left()
            }
            return deserialize<HarSoknadUnderBehandlingResponse>(responseBody).harSoknadUnderBehandling.right()
        }.mapLeft {
            // Either.catch slipper igjennom CancellationException som er ønskelig.
            logger.error(it) { "Feil ved sjekk av åpne søknader mot tiltakspenger-saksbehandling-api. Se sikkerlogg for detaljer." }
            Sikkerlogg.error(it) { "Feil ved sjekk av åpne søknader mot tiltakspenger-saksbehandling-api. jsonPayload: $jsonPayload, uri: $saksbehandlingApiHarSoknadUnderBehandlingUri" }
            SaksbehandlingApiError
        }
    }

    private fun createRequest(
        uri: URI,
        token: AccessToken,
        jsonPayload: String,
    ): HttpRequest? {
        return HttpRequest.newBuilder()
            .uri(uri)
            .timeout(timeout.toJavaDuration())
            .header("Authorization", "Bearer ${token.token}")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build()
    }
}

data object SaksbehandlingApiError

private data class FnrDTO(
    val fnr: String,
)

private data class HarSoknadUnderBehandlingResponse(
    val harSoknadUnderBehandling: Boolean,
)
