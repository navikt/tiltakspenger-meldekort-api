package no.nav.tiltakspenger.meldekort.mottak.infra.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.meldekort.infra.ApplicationContext

/**
 * Samling av routes for mottak av data fra andre systemer, akkurat nå kun fra saksbehandling-api.
 */
fun Routing.mottakModule(applicationContext: ApplicationContext) {
    authenticate(IdentityProvider.AZUREAD.value) {
        route("/saksbehandling") {
            mottakFraSaksbehandlingRoute(
                mottakFraSaksbehandlingService = applicationContext.mottakFraSaksbehandlingService,
            )
        }
    }
}
