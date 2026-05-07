package no.nav.tiltakspenger.meldekort.landingsside.infra.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.infra.ApplicationContext
import no.nav.tiltakspenger.meldekort.landingsside.FellesLandingssideService

/**
 * Endepunkter som kalles fra felles landingsside for meldekortytelsene.
 * Eier auth-provider [IdentityProvider.TOKENX] og path-prefiks `/landingsside`.
 */
fun Routing.landingssideModule(applicationContext: ApplicationContext) {
    authenticate(IdentityProvider.TOKENX.value) {
        route("/landingsside") {
            fellesLandingssideRoutes(
                fellesLandingssideService = applicationContext.fellesLandingssideService,
            )
        }
    }
}

/**
 * Response DTO: [LandingssideStatusDTO]
 */
internal fun Route.fellesLandingssideRoutes(
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

        call.respond(HttpStatusCode.OK, landingssideStatus.tilLandingssideStatusDTO())
    }
}
