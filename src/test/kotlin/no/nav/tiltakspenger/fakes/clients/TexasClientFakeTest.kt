package no.nav.tiltakspenger.fakes.clients

import com.auth0.jwt.JWT
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.libs.texas.client.TexasIntrospectionResponse
import java.time.Instant

/**
 * Fake av [TexasClient].
 *
 * I motsetning til en fake som er låst til én bruker, dekoder denne selve bearer-token-et (en ekte JWT bygget av [no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator]) og speiler claimsene tilbake i introspeksjonen — slik den ekte Texas-tjenesten gjør.
 * Hvilken person et kall autentiseres som, styres altså av `pid`-claimet i token-et.
 *
 * Konsekvens: en test-kontekst er ikke knyttet til ett bestemt fødselsnummer.
 * Vil du teste flere personer i samme test, send ulike `fnr` til route-hjelperne (de bygger token med riktig `pid`).
 */
class TexasClientFakeTest : TexasClient {
    override suspend fun introspectToken(
        token: String,
        identityProvider: IdentityProvider,
    ): TexasIntrospectionResponse {
        val decoded = runCatching { JWT.decode(token) }.getOrNull()
            ?: return TexasIntrospectionResponse(
                active = false,
                error = "Kunne ikke dekode token",
                groups = null,
                roles = null,
                other = emptyMap(),
            )

        val other = buildMap<String, Any?> {
            // Claims bruker-/saksbehandler-routene leser via no.nav.tiltakspenger.libs.texas.*
            decoded.getClaim("pid").asString()?.let { put("pid", it) }
            decoded.getClaim("idtyp").asString()?.let { put("idtyp", it) }
            decoded.getClaim("azp_name").asString()?.let { put("azp_name", it) }
            decoded.getClaim("azp").asString()?.let { put("azp", it) }
            decoded.getClaim("acr").asString()?.let { put("acr", it) }
        }

        return TexasIntrospectionResponse(
            active = true,
            error = null,
            groups = decoded.getClaim("groups").asList(String::class.java),
            roles = decoded.getClaim("roles").asList(String::class.java),
            other = other,
        )
    }

    override suspend fun getSystemToken(
        audienceTarget: String,
        identityProvider: IdentityProvider,
        rewriteAudienceTarget: Boolean,
        skipCache: Boolean,
    ): AccessToken {
        return accessToken()
    }

    override suspend fun exchangeToken(
        userToken: String,
        audienceTarget: String,
        identityProvider: IdentityProvider,
        skipCache: Boolean,
    ): AccessToken {
        return accessToken()
    }

    private fun accessToken(): AccessToken = AccessToken(
        token = "asdf",
        expiresAt = Instant.now().plusSeconds(3600),
    )
}
