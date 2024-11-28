package no.nav.tiltakspenger.meldekort.clients

interface TexasHttpClient {
    suspend fun introspectToken(
        accessToken: String,
    ): TokenIntrospectionResponse
}