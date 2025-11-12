package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.domene.AlleMeldekortDTO
import no.nav.tiltakspenger.meldekort.domene.Bruker
import no.nav.tiltakspenger.meldekort.domene.tilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.toDto
import no.nav.tiltakspenger.meldekort.service.BrukerService
import no.nav.tiltakspenger.meldekort.service.MeldekortService

fun Route.meldekortTilBrukerRoutes(
    meldekortService: MeldekortService,
    brukerService: BrukerService,
) {
    get("meldekort/innsendte") {
        val fnr = call.fnr()

        val bruker = brukerService.hentBruker(fnr)

        val alleMeldekort = if (bruker is Bruker.MedSak) {
            meldekortService.hentInnsendteMeldekort(fnr)
                .map { it.tilMeldekortTilBrukerDTO() }
        } else {
            emptyList()
        }

        call.respond(
            AlleMeldekortDTO(
                bruker = bruker.tilBrukerDTO(),
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
                    meldekort = it.map { it.tilMeldekortTilBrukerDTO() },
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
            call.respond(it.tilMeldekortTilBrukerDTO())
            return@get
        }

        call.respond(HttpStatusCode.NotFound)
    }

    /**
     * Dette apiet brukes også av arena-meldekortløsningen for å se om bruker har meldekort hos oss
     */
    get("bruker") {
        val bruker = brukerService.hentBruker(call.fnr())

        call.respond(bruker.tilBrukerDTO())
    }

    // TODO: Fjern når frontend er oppdatert
    post("/meldeperiode") {
        val periode = deserialize<PeriodeDTO>(call.receiveText())

        meldekortService.hentMeldeperiodeForPeriode(periode.toDomain(), call.fnr()).let {
            call.respond(
                MeldeperiodeResponse(
                    meldeperiodeId = it.meldeperiodeId.toString(),
                    kjedeId = it.kjedeId.verdi,
                    dager = it.dager.toDto(),
                    periode = it.periode.toDTO(),
                    mottattTidspunktSisteMeldekort = it.mottattTidspunktSisteMeldekort,
                    maksAntallDagerForPeriode = it.maksAntallDagerForPeriode,
                ),
            )
        }
    }

    get("/korrigering/{meldekortId}") {
        val meldekortId = call.parameters["meldekortId"]?.let { MeldekortId.fromString(it) }
        if (meldekortId == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        meldekortService.hentSisteMeldeperiodeOgInnsendteMeldekort(meldekortId, call.fnr())
            ?.let { (meldeperiode, meldekort) ->
                call.respond(meldeperiode.tilKorrigeringDTO(meldekort))
            } ?: call.respond(HttpStatusCode.BadRequest)
    }
}
