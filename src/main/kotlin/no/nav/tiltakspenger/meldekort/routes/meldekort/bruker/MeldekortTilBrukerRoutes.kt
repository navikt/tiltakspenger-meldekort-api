package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.auth.fnr
import no.nav.tiltakspenger.meldekort.domene.tilBrukerDTO
import no.nav.tiltakspenger.meldekort.service.MeldekortService

fun Route.meldekortTilBrukerRoutes(
    meldekortService: MeldekortService,
) {
    get("meldekort/{meldekortId}") {
        val meldekortId = call.parameters["meldekortId"]?.let { MeldekortId.fromString(it) }
        if (meldekortId == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        meldekortService.hentForMeldekortId(meldekortId, call.fnr())?.also {
            call.respond(it.tilBrukerDTO())
            return@get
        }

        call.respond(HttpStatusCode.NotFound)
        return@get
    }

    get("neste") {
        val fnr = call.fnr()

        val meldekort =
            meldekortService.hentNesteMeldekortForUtfylling(fnr) ?: meldekortService.hentSisteMeldekort(fnr)
        if (meldekort == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        call.respond(meldekort.tilBrukerDTO())
    }

    get("alle") {
        val alleMeldekort = meldekortService.hentAlleMeldekort(call.fnr()).map {
            it.tilBrukerDTO()
        }

        call.respond(alleMeldekort)
    }
}
