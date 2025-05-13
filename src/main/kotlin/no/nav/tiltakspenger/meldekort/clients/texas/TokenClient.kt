package no.nav.tiltakspenger.meldekort.clients.texas

import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.meldekort.auth.IdentityProvider

interface TokenClient {

    suspend fun introspectToken(
        token: String,
        identityProvider: IdentityProvider,
    ): TokenIntrospectionResponse

    suspend fun getSystemToken(
        audienceTarget: String,
        identityProvider: IdentityProvider,
    ): AccessToken

    suspend fun exchangeToken(
        userToken: String,
        audienceTarget: String,
        identityProvider: IdentityProvider,
    ): AccessToken
}
