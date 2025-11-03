package no.nav.tiltakspenger.fakes

import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasIntrospectionResponse
import java.time.Instant

class TexasClientFakeLokal : TexasClient {
    override suspend fun introspectToken(
        token: String,
        identityProvider: IdentityProvider,
    ): TexasIntrospectionResponse {
        return godkjentResponse(token)
    }

    override suspend fun getSystemToken(
        audienceTarget: String,
        identityProvider: IdentityProvider,
        rewriteAudienceTarget: Boolean,
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

    /**
     *  Bruker tokenet som fnr for enkelt å bytte mellom forskjellige brukere i frontend
     * */
    private fun godkjentResponse(fnrOgToken: String): TexasIntrospectionResponse {
        return TexasIntrospectionResponse(
            active = true,
            error = null,
            groups = null,
            roles = null,
            other = mutableMapOf("pid" to fnrOgToken),
        )
    }
}
