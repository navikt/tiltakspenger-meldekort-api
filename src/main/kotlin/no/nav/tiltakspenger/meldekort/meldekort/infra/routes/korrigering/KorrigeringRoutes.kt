package no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.meldekort.meldekort.korrigering.KorrigerMeldekortService
import java.time.Clock

fun Route.korrigeringRoutes(
    meldekortService: KorrigerMeldekortService,
    clock: Clock,
) {
    hentKorrigeringRoute(meldekortService, clock)
    kanKorrigeresRoute(meldekortService)
    korrigerMeldekortRoute(meldekortService, clock)
}
