package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.domene.AlleMeldekortDTO
import no.nav.tiltakspenger.meldekort.domene.Bruker
import no.nav.tiltakspenger.meldekort.domene.tilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.service.BrukerService
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import java.time.Clock

fun Route.meldekortTilBrukerRoutes(
    meldekortService: MeldekortService,
    brukerService: BrukerService,
    clock: Clock,
) {
    get("meldekort/innsendte") {
        val fnr = call.fnr()

        val bruker = brukerService.hentBruker(fnr)

        val alleMeldekort = if (bruker is Bruker.MedSak) {
            meldekortService.hentInnsendteMeldekort(fnr)
                .map { it.tilMeldekortTilBrukerDTO(clock) }
        } else {
            emptyList()
        }

        call.respond(
            AlleMeldekortDTO(
                bruker = bruker.tilBrukerDTO(clock),
                meldekort = alleMeldekort,
            ),
        )
    }

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

    /**
     * Dette apiet brukes også av arena-meldekortløsningen for å se om bruker har meldekort hos oss
     */
    get("bruker") {
        val bruker = brukerService.hentBruker(call.fnr())

        call.respond(bruker.tilBrukerDTO(clock))
    }

    get("/korrigering/{meldekortId}") {
        val meldekortId = call.parameters["meldekortId"]?.let { MeldekortId.fromString(it) }
        if (meldekortId == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        meldekortService.hentMeldekortOgSisteMeldeperiode(meldekortId, call.fnr())
            .let { (meldeperiode, meldekort) ->
                call.respond(meldeperiode.tilKorrigeringDTO(meldekort, clock))
            }
    }

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

// Lettere å forholde seg til et felt inni et objekt når man skal bruke det i frontend
data class KanKorrigereMeldekortDto(
    val kanKorrigeres: Boolean,
)
