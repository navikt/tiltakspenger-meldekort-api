package no.nav.tiltakspenger.meldekort.routes.meldekort

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.tiltakspenger.meldekort.auth.TexasWallBrukerToken
import no.nav.tiltakspenger.meldekort.auth.TexasWallSystemToken
import no.nav.tiltakspenger.meldekort.clients.texas.TexasClient
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.meldekortFraBrukerRoute
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.meldekortTilBrukerRoutes
import no.nav.tiltakspenger.meldekort.routes.meldekort.saksbehandling.meldeperioderFraSaksbehandlingRoute
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import no.nav.tiltakspenger.meldekort.service.MeldeperiodeService
import java.time.Clock

val logger = KotlinLogging.logger {}

internal fun Route.meldekortRoutes(
    meldekortService: MeldekortService,
    meldeperiodeService: MeldeperiodeService,
    texasClient: TexasClient,
    clock: Clock,
) {
    // Endepunkter som kalles fra saksbehandling-api
    route("/saksbehandling") {
        install(TexasWallSystemToken) {
            client = texasClient
        }

        meldeperioderFraSaksbehandlingRoute(meldeperiodeService)
    }

    // Endepunkter som kalles fra brukers meldekort-frontend
    route("/meldekort/bruker") {
        install(TexasWallBrukerToken) {
            client = texasClient
        }

        meldekortTilBrukerRoutes(meldekortService)
        meldekortFraBrukerRoute(meldekortService, clock)
    }
}
