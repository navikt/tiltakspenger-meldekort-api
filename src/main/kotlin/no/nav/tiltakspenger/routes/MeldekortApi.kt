package no.nav.tiltakspenger.routes

import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.routing.routing
import no.nav.tiltakspenger.auth.initAuthentication
import no.nav.tiltakspenger.routes.meldekort.meldekortRoutes

internal fun Application.meldekortApi() {
    initAuthentication()

    routing {
        healthRoutes()

        authentication {
            meldekortRoutes()
        }
    }
}
