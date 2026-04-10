package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.korrigering

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.service.MeldekortService

fun Route.kanKorrigeresRoute(
    meldekortService: MeldekortService,
) {
    get("/{meldekortId}/kan-korrigeres") {
        val meldekortId = call.parameters["meldekortId"]?.let { MeldekortId.fromString(it) }
        if (meldekortId == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        meldekortService.kanMeldekortKorrigeres(meldekortId, call.fnr())
            .let {
                call.respond(HttpStatusCode.OK, serialize(KanKorrigereMeldekortDto(it)))
            }
    }
}
