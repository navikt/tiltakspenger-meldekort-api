package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.auth.fnr
import no.nav.tiltakspenger.meldekort.domene.AlleMeldekortDTO
import no.nav.tiltakspenger.meldekort.domene.Bruker
import no.nav.tiltakspenger.meldekort.domene.tilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.service.BrukerService
import no.nav.tiltakspenger.meldekort.service.MeldekortService

fun Route.meldekortTilBrukerRoutesV2(
    meldekortService: MeldekortService,
    brukerService: BrukerService,
) {
    get("meldekort/alle") {
        val fnr = call.fnr()

        val bruker = brukerService.hentBruker(fnr)
        if (bruker is Bruker.UtenSak) {
            return@get call.respond(HttpStatusCode.NoContent)
        }

        val alleMeldekort = meldekortService.hentAlleMeldekort(fnr)

        call.respond(
            AlleMeldekortDTO(
                meldekort = alleMeldekort.map { it.tilMeldekortTilBrukerDTO() },
                bruker = bruker.tilBrukerDTO(),
            ),
        )
    }

    get("meldekort/{meldekortId}") {
        val meldekortId = call.parameters["meldekortId"]?.let { MeldekortId.fromString(it) }
        if (meldekortId == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        meldekortService.hentForMeldekortId(meldekortId, call.fnr())?.also {
            call.respond(it.tilMeldekortTilBrukerDTO())
            return@get
        }

        call.respond(HttpStatusCode.NotFound)
    }

    get("bruker") {
        val bruker = brukerService.hentBruker(call.fnr())

        call.respond(bruker.tilBrukerDTO())
    }
}
