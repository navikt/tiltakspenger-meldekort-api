package no.nav.tiltakspenger.meldekort.routes.meldekort

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.auth.TexasWallBrukerToken
import no.nav.tiltakspenger.meldekort.auth.TexasWallSystemToken
import no.nav.tiltakspenger.meldekort.clients.arena.ArenaMeldekortServiceClient
import no.nav.tiltakspenger.meldekort.clients.texas.TexasClient
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.meldekortFraBrukerRoute
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.meldekortTilBrukerRoutes
import no.nav.tiltakspenger.meldekort.routes.meldekort.saksbehandling.meldeperioderFraSaksbehandlingRoute
import no.nav.tiltakspenger.meldekort.routes.meldekort.saksbehandling.sakFraSaksbehandlingRoute
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import no.nav.tiltakspenger.meldekort.service.MeldeperiodeService
import no.nav.tiltakspenger.meldekort.service.SakService
import java.time.Clock

val logger = KotlinLogging.logger {}

fun Route.meldekortRoutes(
    meldekortService: MeldekortService,
    meldeperiodeService: MeldeperiodeService,
    sakService: SakService,
    arenaMeldekortClient: ArenaMeldekortServiceClient,
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

    // Endepunkter som kalles fra brukers meldekort-frontend
    route("/meldekort/bruker") {
        install(TexasWallBrukerToken) {
            client = texasClient
        }

        meldekortTilBrukerRoutes(meldekortService)
        meldekortFraBrukerRoute(meldekortService, clock)
    }

    get("/arenatest/:fnr") {
        val fnr = Fnr.fromString(call.parameters["fnr"]!!)
        logger.info { "Henter meldekort fra arena for fnr $fnr" }

        val nesteMeldekort = arenaMeldekortClient.hentNesteMeldekort(fnr)
        val forrigeMeldekort = arenaMeldekortClient.hentHistoriskeMeldekort(fnr)

        call.respond(listOf(nesteMeldekort, forrigeMeldekort))
    }
}
