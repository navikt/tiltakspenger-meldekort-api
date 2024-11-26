package no.nav.tiltakspenger.meldekort.auth

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.config.ApplicationConfig
import no.nav.security.token.support.v2.asIssuerProps
import no.nav.security.token.support.v2.tokenValidationSupport

fun getSecurityConfig(): ApplicationConfig {
    return ApplicationConfig("security.conf")
}

fun Application.installAuthentication() {
    val config = getSecurityConfig()

    install(Authentication) {
        val issuers = config.asIssuerProps().keys
        issuers.forEach { issuer: String ->
            tokenValidationSupport(
                name = issuer,
                config = config,
//                requiredClaims = RequiredClaims(
//                    issuer = issuer,
//                    claimMap = arrayOf("acr=Level4", "acr=idporten-loa-high"),
//                    combineWithOr = true,
//                ),
            )
        }
    }
}
