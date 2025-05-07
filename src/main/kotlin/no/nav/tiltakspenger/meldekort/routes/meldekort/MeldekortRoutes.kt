package no.nav.tiltakspenger.meldekort.routes.meldekort

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import no.nav.tiltakspenger.meldekort.auth.TexasWallBrukerToken
import no.nav.tiltakspenger.meldekort.auth.TexasWallSystemToken
import no.nav.tiltakspenger.meldekort.clients.texas.TexasClient
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.meldekortFraBrukerRoute
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.meldekortTilBrukerRoutes
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.meldekortTilBrukerRoutesV2
import no.nav.tiltakspenger.meldekort.routes.meldekort.saksbehandling.meldeperioderFraSaksbehandlingRoute
import no.nav.tiltakspenger.meldekort.routes.meldekort.saksbehandling.sakFraSaksbehandlingRoute
import no.nav.tiltakspenger.meldekort.service.BrukerService
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import no.nav.tiltakspenger.meldekort.service.MeldeperiodeService
import no.nav.tiltakspenger.meldekort.service.SakService
import java.time.Clock

val logger = KotlinLogging.logger {}

fun Route.meldekortRoutes(
    meldekortService: MeldekortService,
    meldeperiodeService: MeldeperiodeService,
    sakService: SakService,
    brukerService: BrukerService,
    texasClient: TexasClient,
    clock: Clock,
) {
    // Endepunkter som kalles fra saksbehandling-api
    route("/saksbehandling") {
        install(TexasWallSystemToken) {
            client = texasClient
        }

        meldeperioderFraSaksbehandlingRoute(meldeperiodeService)
        sakFraSaksbehandlingRoute(sakService)
    }

    // DEPRECATED, fjern når frontend er oppdatert
    route("/meldekort/bruker") {
        install(TexasWallBrukerToken) {
            client = texasClient
        }

        meldekortTilBrukerRoutes(meldekortService)
        meldekortFraBrukerRoute(meldekortService, clock)
    }

    // Endepunkter som kalles fra brukers meldekort-frontend
    route("/brukerflate") {
        install(TexasWallBrukerToken) {
            client = texasClient
        }

        meldekortTilBrukerRoutesV2(meldekortService, brukerService)
        meldekortFraBrukerRoute(meldekortService, clock)
    }
}
