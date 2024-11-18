package no.nav.tiltakspenger.routes

import io.ktor.server.application.Application
import io.ktor.server.routing.routing

internal fun Application.meldekortApi() {
    routing {
        healthRoutes()
    }
}
