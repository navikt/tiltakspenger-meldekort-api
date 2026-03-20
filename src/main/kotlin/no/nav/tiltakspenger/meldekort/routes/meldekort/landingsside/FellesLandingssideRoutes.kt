package no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside

import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.fellesLandingssideRoutes() {
    // Serverer status for meldekort for en bruker
    get("/status") {
        // TODO: implement
    }
}
