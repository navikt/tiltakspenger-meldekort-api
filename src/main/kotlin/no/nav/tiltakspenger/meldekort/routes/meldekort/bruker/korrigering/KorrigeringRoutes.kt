package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.korrigering

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import java.time.Clock

fun Route.korrigeringRoutes(
    meldekortService: MeldekortService,
    clock: Clock,
) {
    hentKorrigeringRoute(meldekortService, clock)
    kanKorrigeresRoute(meldekortService)
    korrigerMeldekortRoute(meldekortService, clock)
}
