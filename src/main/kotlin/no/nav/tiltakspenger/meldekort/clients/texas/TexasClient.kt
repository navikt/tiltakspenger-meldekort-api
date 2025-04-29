package no.nav.tiltakspenger.meldekort.clients.texas

import no.nav.tiltakspenger.libs.common.AccessToken

interface TexasClient {

    suspend fun introspectToken(
        token: String,
        identityProvider: String = "tokenx",
    ): TexasIntrospectionResponse

    suspend fun getSystemToken(
        audienceTarget: String,
        identityProvider: String = "azuread",
    ): AccessToken

    suspend fun exchangeToken(
        userToken: String,
        audienceTarget: String,
        identityProvider: String = "tokenx",
    ): AccessToken
}
