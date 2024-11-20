package no.nav.tiltakspenger.routes

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import no.nav.security.token.support.v2.asIssuerProps
import no.nav.tiltakspenger.auth.getSecurityConfig
import no.nav.tiltakspenger.auth.installAuthentication
import no.nav.tiltakspenger.routes.meldekort.meldekortRoutes

internal fun Application.meldekortApi() {
    installAuthentication()

    val issuers = getSecurityConfig().asIssuerProps().keys

    routing {
        healthRoutes()
        authenticate(*issuers.toTypedArray()) {
            meldekortRoutes()
        }
    }
}
