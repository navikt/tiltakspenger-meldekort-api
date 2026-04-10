package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.domene.AlleMeldekortDTO
import no.nav.tiltakspenger.meldekort.domene.Bruker
import no.nav.tiltakspenger.meldekort.domene.tilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortMedSisteMeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.service.BrukerService
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import java.time.Clock

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
