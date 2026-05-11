package no.nav.tiltakspenger.meldekort.bruker.infra.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.meldekort.bruker.BrukerService
import no.nav.tiltakspenger.meldekort.infra.ApplicationContext
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortService
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.hentInnsendteMeldekortRoute
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.hentMeldekortForKjedeRoute
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.hentMeldekortRoute
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.korrigeringRoutes
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.sendInnMeldekortRoute
import java.time.Clock

/**
 * Endepunkter som kalles fra brukers meldekort-frontend.
 * Eier auth-provider [IdentityProvider.TOKENX] og path-prefiks `/brukerfrontend`.
 */
fun Routing.brukerModule(applicationContext: ApplicationContext) {
    authenticate(IdentityProvider.TOKENX.value) {
        route("/brukerfrontend") {
            brukerRoutes(
                meldekortService = applicationContext.meldekortService,
                brukerService = applicationContext.brukerService,
                clock = applicationContext.clock,
            )
        }
    }
}

internal fun Route.brukerRoutes(
    meldekortService: MeldekortService,
    brukerService: BrukerService,
    clock: Clock,
) {
    hentInnsendteMeldekortRoute(meldekortService, brukerService, clock)
    hentMeldekortForKjedeRoute(meldekortService, clock)
    hentMeldekortRoute(meldekortService, clock)
    hentBrukerRoute(brukerService, clock)
    sendInnMeldekortRoute(meldekortService)
    korrigeringRoutes(meldekortService, clock)
}
