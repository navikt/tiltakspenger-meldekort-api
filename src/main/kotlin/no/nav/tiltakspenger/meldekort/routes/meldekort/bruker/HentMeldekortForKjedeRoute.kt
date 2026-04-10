package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import java.time.Clock

/**
 * Response DTO: [MeldekortForKjedeDTO]
 */
fun Route.hentMeldekortForKjedeRoute(
    meldekortService: MeldekortService,
    clock: Clock,
) {
    get("kjede/{kjedeId}") {
        val kjedeId = call.parameters["kjedeId"]?.let { MeldeperiodeKjedeId(it.replace("_", "/")) }
        if (kjedeId == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        meldekortService.hentMeldekortForKjede(kjedeId, call.fnr()).let {
            call.respond(
                HttpStatusCode.OK,
                MeldekortForKjedeDTO(
                    kjedeId = it.kjedeId?.toString(),
                    periode = it.kjedeId?.periode?.toDTO(),
                    meldekort = it.map { it.tilMeldekortTilBrukerDTO(clock) },
                ),
            )
        }
    }
}
