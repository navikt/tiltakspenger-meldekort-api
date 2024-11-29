package no.nav.tiltakspenger.meldekort.clients

import arrow.core.Either
import arrow.core.getOrElse
import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.future.await
import no.nav.tiltakspenger.libs.json.lesTre
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.meldekort.Configuration
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import no.nav.tiltakspenger.libs.json.deserialize

sealed class TokenResponse {
    data class Success(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("expires_in")
        val expiresInSeconds: Int,
    ) : TokenResponse()

    data class Error(
        val error: TokenErrorResponse,
        val status: HttpStatusCode,
    ) : TokenResponse()
}

data class TokenErrorResponse(
    val error: String,
    @JsonProperty("error_description")
    val errorDescription: String,
)

data class TokenIntrospectionResponse(
    val active: Boolean,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val error: String?,
    @JsonAnySetter @get:JsonAnyGetter
    val other: Map<String, Any?> = mutableMapOf(),
)

class TexasHttpClientImpl(
    connectTimeout: kotlin.time.Duration = 1.seconds,
    private val timeout: kotlin.time.Duration = 6.seconds,
) : TexasHttpClient {

    private val client = HttpClient.newBuilder()
        .connectTimeout(connectTimeout.toJavaDuration())
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    private fun systemTokenFormData(audienceTarget: String): String {
        val urlEncodedAudienceTarget = URLEncoder.encode(audienceTarget, StandardCharsets.UTF_8)
        return "identity_provider=maskinporten&target=$urlEncodedAudienceTarget"
    }

    private fun introspectFormData(accessToken: String): String {
        val urlEncodedAccessToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
        return "identity_provider=tokenx&token=$urlEncodedAccessToken"
    }

    override suspend fun introspectToken(
        accessToken: String,
    ): TokenIntrospectionResponse {
        return Either.catch {
            val uri = URI.create(Configuration.naisTokenIntrospectionEndpoint)
            val formData = introspectFormData(accessToken)
            val request = createRequest(formData, uri)
            val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
            val jsonResponse = httpResponse.body()
            val status = httpResponse.statusCode()
            if (status != 200) {
                sikkerlogg.error("Fikk ikke validert token. Status: $status. jsonResponse: $jsonResponse. uri: $uri.")
                throw RuntimeException("Fikk ikke validert token. Status: $status. uri: $uri. Se sikkerlogg for detaljer.")
            }
            Either.catch {
                val jsonNode = lesTre(jsonResponse)

                val active = jsonNode["active"].asBoolean()
                val error = jsonNode["error"]?.asText()
                val knownFields = setOf("active", "error")

                val otherFields = jsonNode.fields().asSequence()
                    .filter { it.key !in knownFields }
                    .associate { it.key to it.value }

                TokenIntrospectionResponse(
                    active = active,
                    error = error,
                    other = otherFields,
                )
            }.getOrElse {
                sikkerlogg.error(it) { "Feil ved parsing av respons fra Texas. status: $status, jsonResponse: $jsonResponse. uri: $uri" }
                throw RuntimeException("Feil ved parsing av respons fra Texas. status: $status. Se sikkerlogg for detaljer.")
            }
        }.getOrElse {
            // Either.catch slipper igjennom CancellationException som er ønskelig.
            sikkerlogg.error(it) { "Ukjent feil ved kall mot Texas. Message: ${it.message}" }
            throw RuntimeException("Feil ved henting av systemtoken. Se sikkerlogg for detaljer.")
        }
    }

    override suspend fun getSystemToken(audienceTarget: String): TokenResponse {
        return Either.catch {
            val uri = URI.create(Configuration.naisTokenEndpoint)
            val formData = systemTokenFormData(audienceTarget)
            val request = createRequest(formData, uri)
            val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
            val jsonResponse = httpResponse.body()
            val status = httpResponse.statusCode()
            if (status != 200) {
                val tokenResponseError = deserialize<TokenResponse.Error>(jsonResponse)
                sikkerlogg.error(
                    """ 
                    Fikk ikke hentet systemtoken. Status: ${tokenResponseError.status}.
                    error: ${tokenResponseError.error.error}
                    errordescription: ${tokenResponseError.error.errorDescription} . uri: $uri 
                """
                )
                throw RuntimeException("Fikk ikke hentet systemtoken. Status: ${tokenResponseError.status}. uri: $uri. Se sikkerlogg for detaljer.")
            }
            Either.catch {
                deserialize<TokenResponse.Success>(jsonResponse)
            }.getOrElse {
                sikkerlogg.error(it) { "Feil ved parsing av respons fra Texas. status: $status, jsonResponse: $jsonResponse. uri: $uri" }
                throw RuntimeException("Feil ved parsing av respons fra Texas. status: $status. Se sikkerlogg for detaljer.")
            }
        }.getOrElse {
            // Either.catch slipper igjennom CancellationException som er ønskelig.
            sikkerlogg.error(it) { "Ukjent feil ved kall mot Texas. Message: ${it.message}" }
            throw RuntimeException("Feil ved henting av systemtoken. Se sikkerlogg for detaljer.")
        }
    }

    private fun createRequest(
        formData: String,
        uri: URI,
    ): HttpRequest? {
        return HttpRequest.newBuilder()
            .uri(uri)
            .timeout(timeout.toJavaDuration())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formData))
            .build()
    }
}
