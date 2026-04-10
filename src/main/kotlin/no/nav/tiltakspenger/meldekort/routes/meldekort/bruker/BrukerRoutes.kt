package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker

import io.ktor.server.routing.Route
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.korrigering.korrigeringRoutes
import no.nav.tiltakspenger.meldekort.service.BrukerService
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import java.time.Clock

fun Route.brukerRoutes(
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
