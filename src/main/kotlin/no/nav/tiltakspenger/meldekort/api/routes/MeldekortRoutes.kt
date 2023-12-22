package no.nav.tiltakspenger.meldekort.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import no.nav.tiltakspenger.meldekort.api.domene.Status
import no.nav.tiltakspenger.meldekort.api.domene.Tiltak
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import no.nav.tiltakspenger.meldekort.api.routes.dto.DayHasBegunDTO
import no.nav.tiltakspenger.meldekort.api.routes.dto.MeldekortDagDTO
import no.nav.tiltakspenger.meldekort.api.routes.dto.MeldekortGrunnlagDTO
import no.nav.tiltakspenger.meldekort.api.routes.dto.MeldekortMedTiltakDTO
import no.nav.tiltakspenger.meldekort.api.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.meldekort.api.routes.dto.StatusDTO
import no.nav.tiltakspenger.meldekort.api.routes.dto.TiltakDTO
import no.nav.tiltakspenger.meldekort.api.service.MeldekortService
import java.time.DayOfWeek
import java.util.UUID

private val LOG = KotlinLogging.logger {}

private const val MELDEKORT_PATH = "/meldekort"
fun Route.meldekort(
    meldekortService: MeldekortService,
) {
//    post("$MELDEKORT_PATH/opprett") {
//        LOG.info("Motatt request på $MELDEKORT_PATH")
//        val meldekortDTO = call.receive<Meldekort.Åpent>()
//        meldekortService.opprettMeldekort(meldekortDTO)
//    }

    get("$MELDEKORT_PATH/hent/{meldekortId}") {
        LOG.info("Motatt request på $MELDEKORT_PATH/hent/{meldekortId}")
        val meldekortIdent = call.parameters["meldekortId"]
//        if (meldekortIdent != null) {
//            call.respond(status = HttpStatusCode.OK, message = meldekortService.hentMeldekort(meldekortIdent))
//        } else {
//            throw NotFoundException("Meldekort med ident:$meldekortIdent eksisterer ikke i databasen")
//        }
    }

    post("$MELDEKORT_PATH/oppdaterDag") {
        val dto = call.receive<MeldekortDagDTO>()

        // TODO() validering av felter og tilgangsstyring av hvem som får oppdatere
        // kan alle saksbehandlere oppdatere?
        // hvis denne skal kunne kalles fra en bruker må vi validere at meldekortet er denne brukeren sitt eller en egen rute
        // validere at man ikke setter en tiltakId som ikke tilhører meldekortet?
        meldekortService.oppdaterMeldekortDag(
            meldekortId = UUID.fromString(dto.meldekortId),
            tiltakId = UUID.fromString(dto.tiltakId),
            dato = dto.dato,
            status = MeldekortDagStatus.valueOf(dto.status),
        )

        call.respond(message = "{}", status = HttpStatusCode.OK)
    }

    get("$MELDEKORT_PATH/hentAlleForBehandling/{behandlingId}") {
        LOG.info("Motatt request på $MELDEKORT_PATH/hentAlleForBehandling/behandlingId")
        val behandlingId = call.parameters["behandlingId"]
            ?: return@get call.respond(message = "behandlingId mangler", status = HttpStatusCode.NotFound)
        val grunnlag = meldekortService.hentGrunnlagForBehandling(behandlingId) ?: return@get call.respond(
            message = "Fant ikke grunnlag for behandlingId: $behandlingId",
            status = HttpStatusCode.NotFound,
        )
        val meldekort = meldekortService.hentAlleMeldekortene(grunnlag.id)
        LOG.info(meldekort.toString())
        val dto = meldekort.map {
            MeldekortMedTiltakDTO(
                id = it.id.toString(),
                fom = it.fom,
                tom = it.tom,
                status = when (it) {
                    is Meldekort.Innsendt -> "INNSENDT"
                    is Meldekort.Åpent -> "ÅPENT"
                },
                meldekortdager = it.meldekortDager,
                tiltak = grunnlag.tiltak.map { tiltak ->
                    TiltakDTO(
                        periodeDTO = PeriodeDTO(
                            fra = tiltak.periode.fra,
                            til = tiltak.periode.til,
                        ),
                        typeBeskrivelse = tiltak.typeBeskrivelse,
                        typeKode = tiltak.typeKode,
                        antDagerIUken = tiltak.antDagerIUken,
                    )
                },
            )
        }
        call.respond(status = HttpStatusCode.OK, dto)
    }

    post("$MELDEKORT_PATH/nyDag") {
        // meldekortService.settGamleMeldekortTilIkkeAktiv()
        LOG.info("Det er en ny dag")
        val nyDag = call.receive<DayHasBegunDTO>().date
        LOG.info("Den ny dagen er: $nyDag")
        if (nyDag.dayOfWeek == DayOfWeek.MONDAY) {
            LOG.info { "Det er Mandag!!! Kanskje vi skal generere meldekort her??" }
            meldekortService.genererMeldekort(nyDag)
        }
        call.respond(message = "OK", status = HttpStatusCode.OK)
    }

    post("$MELDEKORT_PATH/grunnlag") {
        val dto = call.receive<MeldekortGrunnlagDTO>()
        LOG.info { "Vi fikk nytt grunnlag : $dto" }

        meldekortService.mottaGrunnlag(mapGrunnlag(dto))
        call.respond(message = "OK", status = HttpStatusCode.OK)
    }
}

private fun mapGrunnlag(dto: MeldekortGrunnlagDTO): MeldekortGrunnlag {
    return MeldekortGrunnlag(
        id = UUID.randomUUID(),
        vedtakId = dto.vedtakId,
        behandlingId = dto.behandlingId,
        status = when (dto.status) {
            StatusDTO.AKTIV -> Status.AKTIV
            StatusDTO.IKKE_AKTIV -> Status.IKKE_AKTIV
        },
        vurderingsperiode = Periode(
            fra = dto.vurderingsperiode.fra,
            til = dto.vurderingsperiode.til,
        ),
        tiltak = dto.tiltak.map {
            Tiltak(
                id = UUID.randomUUID(),
                periode = Periode(
                    fra = it.periodeDTO.fra,
                    til = it.periodeDTO.til,
                ),
                typeBeskrivelse = it.typeBeskrivelse,
                typeKode = it.typeKode,
                antDagerIUken = it.antDagerIUken,
            )
        },
    )
}
