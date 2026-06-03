package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.bruker.Bruker
import no.nav.tiltakspenger.meldekort.bruker.BrukerService
import no.nav.tiltakspenger.meldekort.bruker.infra.routes.tilBrukerDTO
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortService
import no.nav.tiltakspenger.meldekort.meldekort.infra.AlleMeldekortDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.tilMeldekortMedSisteMeldeperiodeDTO
import java.time.Clock

/**
 * Henter tp-sak eller arena-sak. Frontend skiller på disse 2 for å vise forskjellige tekster/lenker.
 * Response DTO: [AlleMeldekortDTO]
 */
fun Route.hentInnsendteMeldekortRoute(
    meldekortService: MeldekortService,
    brukerService: BrukerService,
    clock: Clock,
) {
    get("meldekort/innsendte") {
        val fnr = call.fnr()

        val bruker = brukerService.hentBruker(fnr)

        val alleMeldekort = if (bruker is Bruker.MedSak) {
            meldekortService.hentInnsendteMeldekort(fnr)
        } else {
            emptyList()
        }

        call.respond(
            AlleMeldekortDTO(
                bruker = bruker.tilBrukerDTO(clock),
                meldekortMedSisteMeldeperiode = alleMeldekort.map { it.tilMeldekortMedSisteMeldeperiodeDTO(clock) },
            ),
        )
    }
}
