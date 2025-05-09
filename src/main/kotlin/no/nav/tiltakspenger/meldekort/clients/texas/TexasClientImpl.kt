package no.nav.tiltakspenger.meldekort.clients.texas

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.meldekort.auth.TexasIdentityProvider
import no.nav.tiltakspenger.meldekort.clients.httpClientApache

class TexasClientImpl(
    private val introspectionUrl: String,
    private val tokenUrl: String,
    private val tokenExchangeUrl: String,
    private val httpClient: HttpClient = httpClientApache(),
) : TexasClient {
    private val logger = KotlinLogging.logger {}

    override suspend fun introspectToken(token: String, identityProvider: TexasIdentityProvider): TexasIntrospectionResponse {
        val texasIntrospectionRequest = TexasIntrospectionRequest(
            identityProvider = identityProvider.value,
            token = token,
        )

        try {
            val response =
                httpClient.post(introspectionUrl) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(texasIntrospectionRequest)
                }
            return response.body<TexasIntrospectionResponse>()
        } catch (e: Exception) {
            if (e is ResponseException) {
                logger.error { "Kall for autentisering mot Texas feilet, responskode ${e.response.status}" }
            }
            logger.error { "Kall for autentisering mot Texas feilet, se sikker logg for detaljer" }
            sikkerlogg.error(e) { "Kall for autentisering mot Texas feilet, melding: ${e.message}" }
            throw e
        }
    }

    override suspend fun getSystemToken(audienceTarget: String, identityProvider: TexasIdentityProvider): AccessToken {
        val target = audienceTarget.replace(':', '.')
        val texasTokenRequest = TexasTokenRequest(
            identityProvider = identityProvider.value,
            target = "api://$target/.default",
        )

        try {
            val response =
                httpClient.post(tokenUrl) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(texasTokenRequest)
                }
            val texasTokenResponse = response.body<TexasTokenResponse>()
            return texasTokenResponse.toAccessToken()
        } catch (e: Exception) {
            if (e is ResponseException) {
                logger.error { "Kall for å hente token mot Texas feilet, responskode ${e.response.status}" }
            }
            logger.error { "Kall å hente token mot Texas feilet, se sikker logg for detaljer" }
            sikkerlogg.error(e) { "Kall å hente token mot Texas feilet, melding: ${e.message}" }
            throw e
        }
    }

    override suspend fun exchangeToken(
        userToken: String,
        audienceTarget: String,
        identityProvider: TexasIdentityProvider,
    ): AccessToken {
        val texasExchangeTokenRequest = TexasExchangeTokenRequest(
            identityProvider = identityProvider.value,
            target = audienceTarget,
            userToken = userToken,
        )

        try {
            val response = httpClient.post(tokenExchangeUrl) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(texasExchangeTokenRequest)
            }
            val texasTokenResponse = response.body<TexasTokenResponse>()
            return texasTokenResponse.toAccessToken()
        } catch (e: Exception) {
            if (e is ResponseException) {
                logger.error { "Kall for å veksle token mot Texas feilet, responskode ${e.response.status}" }
            }
            logger.error { "Kall å veksle token mot Texas feilet, se sikker logg for detaljer" }
            sikkerlogg.error(e) { "Kall å veksle token mot Texas feilet, melding: ${e.message}" }
            throw e
        }
    }
}
