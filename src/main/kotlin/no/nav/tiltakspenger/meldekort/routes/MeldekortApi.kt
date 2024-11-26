package no.nav.tiltakspenger.meldekort.routes

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import no.nav.security.token.support.v2.asIssuerProps
import no.nav.tiltakspenger.meldekort.auth.getSecurityConfig
import no.nav.tiltakspenger.meldekort.auth.installAuthentication
import no.nav.tiltakspenger.meldekort.context.ApplicationContext
import no.nav.tiltakspenger.meldekort.routes.meldekort.meldekortRoutes

internal fun Application.meldekortApi(
    applicationContext: ApplicationContext,
) {
    installAuthentication()

    val issuers = getSecurityConfig().asIssuerProps().keys

    routing {
        healthRoutes()
        authenticate(*issuers.toTypedArray()) {
            meldekortRoutes()
        }
    }
}
