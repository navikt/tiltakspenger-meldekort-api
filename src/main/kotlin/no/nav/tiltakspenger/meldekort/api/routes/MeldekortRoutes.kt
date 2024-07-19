package no.nav.tiltakspenger.meldekort.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import no.nav.tiltakspenger.meldekort.api.domene.Personopplysninger
import no.nav.tiltakspenger.meldekort.api.domene.Status
import no.nav.tiltakspenger.meldekort.api.domene.Tiltak
import no.nav.tiltakspenger.meldekort.api.domene.UtfallForPeriode
import no.nav.tiltakspenger.meldekort.api.domene.Utfallsperiode
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import no.nav.tiltakspenger.meldekort.api.routes.dto.MeldekortDagDTO
import no.nav.tiltakspenger.meldekort.api.routes.dto.MeldekortGrunnlagDTO
import no.nav.tiltakspenger.meldekort.api.routes.dto.MeldekortUtenDagerDTO
import no.nav.tiltakspenger.meldekort.api.routes.dto.StatusDTO
import no.nav.tiltakspenger.meldekort.api.routes.dto.UtfallForPeriodeDTO
import no.nav.tiltakspenger.meldekort.api.service.MeldekortService
import java.util.UUID

private val LOG = KotlinLogging.logger {}

private const val MELDEKORT_PATH = "/meldekort"
fun Route.meldekort(
    meldekortService: MeldekortService,
) {
    get("$MELDEKORT_PATH/hentMeldekort/{meldekortId}") {
        LOG.info("Motatt request på $MELDEKORT_PATH/hentMeldekort/{meldekortId}")
        val meldekortId = call.parameters["meldekortId"]
            ?: return@get call.respond(message = "meldekortId mangler", status = HttpStatusCode.NotFound)
        val dto = meldekortService.hentMeldekort(UUID.fromString(meldekortId))
        checkNotNull(dto) { "Meldekort med ident:$meldekortId eksisterer ikke i databasen" }
        call.respond(status = HttpStatusCode.OK, message = dto)
    }

    get("$MELDEKORT_PATH/hentBeregning/{meldekortId}") {
        val meldekortId = call.parameters["meldekortId"]?.let { UUID.fromString(it) }
            ?: return@get call.respond(message = "meldekortId mangler", status = HttpStatusCode.NotFound)

        LOG.info { "Motatt request på $MELDEKORT_PATH/hentBeregning/$meldekortId" }

        val dto = meldekortService.hentMeldekortBeregning(meldekortId)

        call.respond(status = HttpStatusCode.OK, message = dto)
    }

    post("$MELDEKORT_PATH/oppdaterDag") {
        val dto = call.receive<MeldekortDagDTO>()
        LOG.info("Motatt request på $MELDEKORT_PATH/hentAlleForBehandling/behandlingId")

        // TODO() validering av felter og tilgangsstyring av hvem som får oppdatere
        // kan alle saksbehandlere oppdatere?
        // hvis denne skal kunne kalles fra en bruker må vi validere at meldekortet er denne brukeren sitt eller en egen rute
        // validere at man ikke setter en tiltakId som ikke tilhører meldekortet?
        meldekortService.oppdaterMeldekortDag(
            meldekortId = UUID.fromString(dto.meldekortId),
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
            MeldekortUtenDagerDTO(
                id = it.id.toString(),
                fom = it.fom,
                tom = it.tom,
                status = it.status.toString(),
            )
        }
        call.respond(status = HttpStatusCode.OK, dto)
    }

    // TODO jah: Denne skal slettes når vedtak-rivers ikke lenger kaller oss.
    post("$MELDEKORT_PATH/nyDag") {
        call.respond(message = "OK", status = HttpStatusCode.OK)
    }

    post("$MELDEKORT_PATH/grunnlag") {
        val dto = call.receive<MeldekortGrunnlagDTO>()
        LOG.info { "Vi fikk nytt grunnlag : $dto" }

        meldekortService.mottaGrunnlag(mapGrunnlag(dto))
        call.respond(message = "OK", status = HttpStatusCode.OK)
    }

    post("$MELDEKORT_PATH/godkjenn/{meldekortId}") {
        LOG.info { "Motatt request på $MELDEKORT_PATH/godkjenn/{meldekortId}" }
        val meldekortId = call.parameters["meldekortId"]?.let { UUID.fromString(it) }
            ?: return@post call.respond(message = "meldekortId mangler", status = HttpStatusCode.NotFound)

        // Denne kan vi kanskje hente fra token på sikt?
        val saksbehandler = "Z123456" // call.receive<GodkjennDTO>().saksbehandler

        LOG.info { "Meldekort med id $meldekortId skal godkjenns" }
        meldekortService.godkjennMeldekort(meldekortId, saksbehandler)

        call.respond(message = "OK", status = HttpStatusCode.OK)
    }
}

private fun mapGrunnlag(dto: MeldekortGrunnlagDTO): MeldekortGrunnlag {
    return MeldekortGrunnlag(
        id = UUID.randomUUID(),
        sakId = dto.sakId,
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
        personopplysninger = Personopplysninger(
            fornavn = dto.personopplysninger.fornavn,
            etternavn = dto.personopplysninger.etternavn,
            ident = dto.personopplysninger.ident,
        ),
        utfallsperioder = dto.utfallsperioder.map {
            Utfallsperiode(
                fom = it.fom,
                tom = it.tom,
                antallBarn = it.antallBarn,
                utfall = when (it.utfall) {
                    UtfallForPeriodeDTO.GIR_RETT_TILTAKSPENGER -> UtfallForPeriode.GIR_RETT_TILTAKSPENGER
                    UtfallForPeriodeDTO.GIR_IKKE_RETT_TILTAKSPENGER -> UtfallForPeriode.GIR_IKKE_RETT_TILTAKSPENGER
                    UtfallForPeriodeDTO.KREVER_MANUELL_VURDERING -> UtfallForPeriode.KREVER_MANUELL_VURDERING
                },

            )
        },
    )
}
