package no.nav.tiltakspenger.meldekort.bruker.infra.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.meldekort.infra.ApplicationContext

/**
 * Endepunkter for bruker, kalt fra brukers meldekort-frontend (og arena-meldekortløsningen).
 * Bruker auth-provider [IdentityProvider.TOKENX] og path-prefiks `/brukerfrontend` (delt med
 * [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.meldekortModule] – se TODO der).
 */
fun Routing.brukerModule(applicationContext: ApplicationContext) {
    authenticate(IdentityProvider.TOKENX.value) {
        route("/brukerfrontend") {
            hentBrukerRoute(
                brukerService = applicationContext.brukerService,
                clock = applicationContext.clock,
            )
        }
    }
}
