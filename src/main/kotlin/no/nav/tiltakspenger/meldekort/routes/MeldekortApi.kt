package no.nav.tiltakspenger.meldekort.routes

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.request.path
import io.ktor.server.routing.routing
import no.nav.security.token.support.v2.asIssuerProps
import no.nav.tiltakspenger.meldekort.auth.getSecurityConfig
import no.nav.tiltakspenger.meldekort.auth.installAuthentication
import no.nav.tiltakspenger.meldekort.context.ApplicationContext
import no.nav.tiltakspenger.meldekort.routes.meldekort.meldekortRoutes
import org.slf4j.event.Level

internal fun Application.meldekortApi(
    applicationContext: ApplicationContext,
) {
    installAuthentication()
    install(CallLogging) {
        level = Level.INFO
        filter { call ->
            !call.request.path().startsWith("/isalive") &&
                !call.request.path().startsWith("/isready") &&
                !call.request.path().startsWith("/metrics")
        }
    }

    val issuers = getSecurityConfig().asIssuerProps().keys

    routing {
        healthRoutes()
        authenticate(*issuers.toTypedArray()) {
            meldekortRoutes(meldekortService = applicationContext.meldekortService)
        }
    }
}
