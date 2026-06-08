package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.meldekort.HentMeldekortService
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.tilMeldekortTilBrukerDTO
import java.time.Clock

/**
 * Response DTO: [MeldekortTilBrukerDTO]
 */
fun Route.hentMeldekortRoute(
    meldekortService: HentMeldekortService,
    clock: Clock,
) {
    get("meldekort/{meldekortId}") {
        val meldekortId = call.parameters["meldekortId"]?.let { MeldekortId.fromString(it) }
        if (meldekortId == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        meldekortService.hentForMeldekortId(meldekortId, call.fnr())?.also {
            call.respond(it.tilMeldekortTilBrukerDTO(clock))
            return@get
        }

        call.respond(HttpStatusCode.NotFound)
    }
}
