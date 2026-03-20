package no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.service.MeldekortService

fun Route.fellesLandingssideRoutes(
    meldekortService: MeldekortService,
) {
    // Serverer status for meldekort for en bruker
    get("/status") {
        val fnr = call.fnr()
        val landingssideStatus = meldekortService.hentLandingssideStatus(fnr)
        call.respond(HttpStatusCode.OK, landingssideStatus)
    }
}
