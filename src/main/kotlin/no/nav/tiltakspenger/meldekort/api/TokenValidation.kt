package no.nav.tiltakspenger.meldekort.api

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import no.nav.security.token.support.v2.IssuerConfig
import no.nav.security.token.support.v2.RequiredClaims
import no.nav.security.token.support.v2.TokenSupportConfig
import no.nav.security.token.support.v2.tokenValidationSupport

fun Application.installTokenValidation(config: TokenValidationConfig) {
    install(Authentication) {
        tokenValidationSupport(
            name = config.name,
            config = TokenSupportConfig(
                IssuerConfig(
                    name = config.name,
                    discoveryUrl = config.discoveryUrl,
                    acceptedAudience = config.acceptedAudience,
                ),
            ),
            // Required claims for alle azure-tokens
            requiredClaims = RequiredClaims(
                issuer = "azure",
                claimMap = arrayOf("NAVident=*", "idtyp=app"),
                combineWithOr = true,
            ),
        )
    }
}

data class TokenValidationConfig(
    val name: String,
    val discoveryUrl: String,
    val acceptedAudience: List<String>,
)
