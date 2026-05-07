package no.nav.tiltakspenger.meldekort.bruker.infra.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.bruker.BrukerService
import java.time.Clock

/**
 * Response DTO: [BrukerDTO]
 */
fun Route.hentBrukerRoute(
    brukerService: BrukerService,
    clock: Clock,
) {
    /*
     Dette apiet brukes også av arena-meldekortløsningen for å se om bruker har meldekort hos oss
     */
    get("bruker") {
        val bruker = brukerService.hentBruker(call.fnr())

        call.respond(bruker.tilBrukerDTO(clock))
    }
}
