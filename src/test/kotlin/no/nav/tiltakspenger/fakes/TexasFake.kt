package no.nav.tiltakspenger.fakes

import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.meldekort.auth.TexasIdentityProvider
import no.nav.tiltakspenger.meldekort.clients.texas.TexasClient
import no.nav.tiltakspenger.meldekort.clients.texas.TexasIntrospectionResponse

const val TEXAS_FAKE_FNR = "12345678911"

class TexasFake : TexasClient {
    override suspend fun introspectToken(token: String, identityProvider: TexasIdentityProvider): TexasIntrospectionResponse {
        return godkjentResponse()
    }

    override suspend fun getSystemToken(
        audienceTarget: String,
        identityProvider: TexasIdentityProvider,
    ): AccessToken {
        TODO("Not yet implemented")
    }

    override suspend fun exchangeToken(
        userToken: String,
        audienceTarget: String,
        identityProvider: TexasIdentityProvider,
    ): AccessToken {
        TODO("Not yet implemented")
    }

    private fun godkjentResponse(): TexasIntrospectionResponse {
        return TexasIntrospectionResponse(
            active = true,
            error = null,
            other = mutableMapOf("pid" to TEXAS_FAKE_FNR),
        )
    }
}
