package no.nav.tiltakspenger.meldekort.clients.texas

import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.meldekort.auth.TexasIdentityProvider

interface TexasClient {

    suspend fun introspectToken(
        token: String,
        identityProvider: TexasIdentityProvider,
    ): TexasIntrospectionResponse

    suspend fun getSystemToken(
        audienceTarget: String,
        identityProvider: TexasIdentityProvider,
    ): AccessToken

    suspend fun exchangeToken(
        userToken: String,
        audienceTarget: String,
        identityProvider: TexasIdentityProvider,
    ): AccessToken
}
