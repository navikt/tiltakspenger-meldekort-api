package no.nav.tiltakspenger.fakes

import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.meldekort.auth.IdentityProvider
import no.nav.tiltakspenger.meldekort.clients.texas.TokenClient
import no.nav.tiltakspenger.meldekort.clients.texas.TokenIntrospectionResponse
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR
import java.time.Instant

class TokenClientFake : TokenClient {
    override suspend fun introspectToken(
        token: String,
        identityProvider: IdentityProvider,
    ): TokenIntrospectionResponse {
        return godkjentResponse()
    }

    override suspend fun getSystemToken(
        audienceTarget: String,
        identityProvider: IdentityProvider,
    ): AccessToken {
        return accessToken()
    }

    override suspend fun exchangeToken(
        userToken: String,
        audienceTarget: String,
        identityProvider: IdentityProvider,
    ): AccessToken {
        return accessToken()
    }

    private fun accessToken(): AccessToken = AccessToken(
        token = "asdf",
        expiresAt = Instant.now().plusSeconds(3600),
        invaliderCache = { },
    )

    private fun godkjentResponse(): TokenIntrospectionResponse {
        return TokenIntrospectionResponse(
            active = true,
            error = null,
            other = mutableMapOf("pid" to FAKE_FNR),
        )
    }
}
