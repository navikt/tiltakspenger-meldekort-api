package no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortService
import java.time.Clock

fun Route.korrigeringRoutes(
    meldekortService: MeldekortService,
    clock: Clock,
) {
    hentKorrigeringRoute(meldekortService, clock)
    kanKorrigeresRoute(meldekortService)
    korrigerMeldekortRoute(meldekortService, clock)
}
