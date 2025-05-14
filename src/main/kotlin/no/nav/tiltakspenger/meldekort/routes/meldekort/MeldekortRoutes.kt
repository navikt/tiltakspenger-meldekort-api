package no.nav.tiltakspenger.meldekort.routes.meldekort

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.tiltakspenger.meldekort.auth.TexasWallBrukerToken
import no.nav.tiltakspenger.meldekort.auth.TexasWallSystemToken
import no.nav.tiltakspenger.meldekort.clients.texas.TokenClient
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.meldekortFraBrukerRoute
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.meldekortTilBrukerRoutes
import no.nav.tiltakspenger.meldekort.routes.meldekort.saksbehandling.sakFraSaksbehandlingRoute
import no.nav.tiltakspenger.meldekort.service.BrukerService
import no.nav.tiltakspenger.meldekort.service.LagreFraSaksbehandlingService
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import java.time.Clock

val logger = KotlinLogging.logger {}

fun Route.meldekortRoutes(
    meldekortService: MeldekortService,
    lagreFraSaksbehandlingService: LagreFraSaksbehandlingService,
    brukerService: BrukerService,
    tokenClient: TokenClient,
    clock: Clock,
) {
    // Endepunkter som kalles fra saksbehandling-api
    route("/saksbehandling") {
        install(TexasWallSystemToken) {
            client = tokenClient
        }

        sakFraSaksbehandlingRoute(lagreFraSaksbehandlingService)
    }

    // Endepunkter som kalles fra brukers meldekort-frontend
    route("/brukerfrontend") {
        install(TexasWallBrukerToken) {
            client = tokenClient
        }

        meldekortTilBrukerRoutes(meldekortService, brukerService)
        meldekortFraBrukerRoute(meldekortService, clock)
    }
}
