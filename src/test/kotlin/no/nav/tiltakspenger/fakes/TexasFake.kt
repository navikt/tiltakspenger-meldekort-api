package no.nav.tiltakspenger.fakes

import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.meldekort.clients.TexasHttpClient
import no.nav.tiltakspenger.meldekort.clients.TokenIntrospectionResponse

class TexasFake : TexasHttpClient {
    override suspend fun introspectToken(accessToken: String, identityProvider: String): TokenIntrospectionResponse {
        return godkjentResponse()
    }

    override suspend fun getSaksbehandlingApiToken(): AccessToken {
        TODO("Not yet implemented")
    }

    fun godkjentResponse(): TokenIntrospectionResponse {
        return TokenIntrospectionResponse(
            active = true,
            error = null,
            other = mutableMapOf("pid" to "12345678901"),
        )
    }
}