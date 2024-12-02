package no.nav.tiltakspenger.meldekort.clients

import arrow.core.Either
import arrow.core.getOrElse
import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.future.await
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.meldekort.Configuration
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

sealed class TokenResponse {
    data class Success(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("expires_in")
        val expiresInSeconds: Long,
    ) : TokenResponse()

    data class Error(
        val error: String,
        @JsonProperty("error_description")
        val errorDescription: String,
    ) : TokenResponse()
}

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
        return "identity_provider=azuread&target=$urlEncodedAudienceTarget"
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
                deserialize<TokenIntrospectionResponse>(jsonResponse)
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

    override suspend fun getSaksbehandlingApiToken(): AccessToken {
        val tokenResponse = getSystemToken(Configuration.saksbehandlingApiAudience) as TokenResponse.Success
        return AccessToken(
            token = tokenResponse.accessToken,
            expiresAt = Instant.now().plusSeconds(tokenResponse.expiresInSeconds),
            invaliderCache = {},
        )
    }

    private suspend fun getSystemToken(audienceTarget: String): TokenResponse {
        return Either.catch {
            val uri = URI.create(Configuration.naisTokenEndpoint)
            val formData = systemTokenFormData(audienceTarget)
            val request = createRequest(formData, uri)
            val httpResponse = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
            val jsonResponse = httpResponse.body()
            val status = httpResponse.statusCode()
            if (status != 200) {
                val tokenResponseError = try {
                    deserialize<TokenResponse.Error>(jsonResponse)
                } catch (e: Exception) {
                    sikkerlogg.error("Kunne ikke parse error JSON: $jsonResponse")

                    TokenResponse.Error(
                        error = "Kunne ikke parse feil-response",
                        errorDescription = e.toString(),
                    )
                }
                sikkerlogg.error(
                    """ 
                    Fikk ikke hentet systemtoken. Status: $status.
                    error: ${tokenResponseError.error}
                    errordescription: ${tokenResponseError.errorDescription} . uri: $uri 
                """,
                )
                throw RuntimeException("Fikk ikke hentet systemtoken. Status: $status. uri: $uri. Se sikkerlogg for detaljer.")
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
