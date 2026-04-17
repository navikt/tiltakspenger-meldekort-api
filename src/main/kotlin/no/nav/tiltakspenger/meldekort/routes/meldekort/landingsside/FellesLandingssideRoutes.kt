package no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.service.FellesLandingssideService

/**
 * Response DTO: [LandingssideStatusDTO]
 */
fun Route.fellesLandingssideRoutes(
    fellesLandingssideService: FellesLandingssideService,
) {
    // Serverer status for meldekort for en bruker
    get("/status") {
        val fnr = call.fnr()
        val landingssideStatus = fellesLandingssideService.hentLandingssideStatus(fnr)

        if (landingssideStatus == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        call.respond(HttpStatusCode.OK, landingssideStatus)
    }
}
