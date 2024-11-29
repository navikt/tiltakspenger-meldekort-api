package no.nav.tiltakspenger.meldekort.clients

import no.nav.tiltakspenger.libs.common.AccessToken

interface TexasHttpClient {
    suspend fun introspectToken(
        accessToken: String,
    ): TokenIntrospectionResponse

    suspend fun getSaksbehandlingApiToken(): AccessToken
}
