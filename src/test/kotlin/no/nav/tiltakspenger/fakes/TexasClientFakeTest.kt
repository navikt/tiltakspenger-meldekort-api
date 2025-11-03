package no.nav.tiltakspenger.fakes

import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasIntrospectionResponse
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR
import java.time.Instant

class TexasClientFakeTest : TexasClient {
    override suspend fun introspectToken(
        token: String,
        identityProvider: IdentityProvider,
    ): TexasIntrospectionResponse {
        return godkjentResponse()
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

    private fun godkjentResponse(): TexasIntrospectionResponse {
        return TexasIntrospectionResponse(
            active = true,
            error = null,
            groups = null,
            roles = null,
            other = mutableMapOf("pid" to FAKE_FNR),
        )
    }
}
