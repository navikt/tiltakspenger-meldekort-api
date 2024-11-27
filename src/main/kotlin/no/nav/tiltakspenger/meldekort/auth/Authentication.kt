package no.nav.tiltakspenger.meldekort.auth

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authentication
import io.ktor.server.config.ApplicationConfig
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import no.nav.security.token.support.v2.asIssuerProps
import no.nav.security.token.support.v2.tokenValidationSupport
import no.nav.tiltakspenger.libs.common.Fnr

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

internal fun ApplicationCall.getFnrString(): String? {
    return this.authentication
        .principal<TokenValidationContextPrincipal>()
        ?.context
        ?.getClaims("tokendings")
        ?.getStringClaim("pid")
}

internal fun ApplicationCall.getFnr(): Fnr? {
    return getFnrString()?.let { Fnr.fromString(it) }
}
