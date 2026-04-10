package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.korrigering

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.service.MeldekortService

/**
 * Response DTO: [MeldekortTilKorrigeringDTO]
 */
fun Route.hentKorrigeringRoute(
    meldekortService: MeldekortService,
    clock: java.time.Clock,
) {
    get("/korrigering/{meldekortId}") {
        val meldekortId = call.parameters["meldekortId"]?.let { MeldekortId.fromString(it) }
        if (meldekortId == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        meldekortService.hentForKorrigering(meldekortId, call.fnr())
            .let { (meldekort, meldeperiode, kanSendeInnHelg) ->
                call.respond(meldeperiode.tilKorrigeringDTO(meldekort, kanSendeInnHelg, clock))
            }
    }
}
