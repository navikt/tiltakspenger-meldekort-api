package no.nav.tiltakspenger.meldekort.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.meldekort.MeldekortGrunnlagDTO
import no.nav.tiltakspenger.libs.meldekort.StatusDTO
import no.nav.tiltakspenger.libs.meldekort.UtfallForPeriodeDTO
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import no.nav.tiltakspenger.meldekort.api.domene.Personopplysninger
import no.nav.tiltakspenger.meldekort.api.domene.Status
import no.nav.tiltakspenger.meldekort.api.domene.Tiltak
import no.nav.tiltakspenger.meldekort.api.domene.UtfallForPeriode
import no.nav.tiltakspenger.meldekort.api.domene.Utfallsperiode
import no.nav.tiltakspenger.meldekort.api.domene.toDomain
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import no.nav.tiltakspenger.meldekort.api.routes.dto.MeldekortDagDTO
import no.nav.tiltakspenger.meldekort.api.routes.dto.MeldekortUtenDagerDTO
import no.nav.tiltakspenger.meldekort.api.routes.dto.toDTO
import no.nav.tiltakspenger.meldekort.api.service.MeldekortService
import no.nav.tiltakspenger.meldekort.api.tilgang.InnloggetBrukerProvider
import no.nav.tiltakspenger.meldekort.api.tilgang.Saksbehandler
import java.lang.Exception
import java.time.LocalDate
import java.util.UUID

private val LOG = KotlinLogging.logger {}

private const val MELDEKORT_PATH = "/meldekort"

fun Route.meldekort(
    meldekortService: MeldekortService,
    innloggetBrukerProvider: InnloggetBrukerProvider,
) {
    get("$MELDEKORT_PATH/hentMeldekort/{meldekortId}") {
        LOG.info("Motatt request på $MELDEKORT_PATH/hentMeldekort/{meldekortId}")
        val saksbehandler: Saksbehandler = innloggetBrukerProvider.krevInnloggetSaksbehandler(call)
        val meldekortId =
            call.parameters["meldekortId"]
                ?: return@get call.respond(message = "meldekortId mangler", status = HttpStatusCode.NotFound)
        val meldekortUUID = try {
            UUID.fromString(meldekortId)
        } catch (e: Exception) {
            throw IllegalArgumentException("Kunne ikke parse meldekortID ($meldekortId) til UUID")
        }
        val meldekort = meldekortService.hentMeldekort(meldekortUUID)
        checkNotNull(meldekort) { "Meldekort med ident:$meldekortId eksisterer ikke i databasen" }
        call.respond(status = HttpStatusCode.OK, message = meldekort.toDTO())
    }

    get("$MELDEKORT_PATH/hentBeregning/{meldekortId}") {
        val saksbehandler: Saksbehandler = innloggetBrukerProvider.krevInnloggetSaksbehandler(call)
        val meldekortId =
            call.parameters["meldekortId"]?.let { UUID.fromString(it) }
                ?: return@get call.respond(message = "meldekortId mangler", status = HttpStatusCode.NotFound)

        val dto = meldekortService.hentMeldekortBeregning(meldekortId)

        call.respond(status = HttpStatusCode.OK, message = dto)
    }

    post("$MELDEKORT_PATH/oppdaterDag") {
        val saksbehandler: Saksbehandler = innloggetBrukerProvider.krevInnloggetSaksbehandler(call)
        val dto = call.receive<MeldekortDagDTO>()

        // TODO() validering av felter og tilgangsstyring av hvem som får oppdatere
        // kan alle saksbehandlere oppdatere?
        // hvis denne skal kunne kalles fra en bruker må vi validere at meldekortet er denne brukeren sitt eller en egen rute
        // validere at man ikke setter en tiltakId som ikke tilhører meldekortet?

        meldekortService.oppdaterMeldekortDag(
            meldekortId = UUID.fromString(dto.meldekortId),
            dato = dto.dato,
            status = dto.status.toDomain(),
        )

        call.respond(message = "{}", status = HttpStatusCode.OK)
    }

    get("$MELDEKORT_PATH/hentAlleForBehandling/{behandlingId}") {
        LOG.info("Motatt request på $MELDEKORT_PATH/hentAlleForBehandling/behandlingId")
        val saksbehandler: Saksbehandler = innloggetBrukerProvider.krevInnloggetSaksbehandler(call)
        val behandlingId =
            call.parameters["behandlingId"]
                ?: return@get call.respond(message = "behandlingId mangler", status = HttpStatusCode.NotFound)
        val grunnlag =
            meldekortService.hentGrunnlagForBehandling(behandlingId) ?: return@get call.respond(
                message = "Fant ikke grunnlag for behandlingId: $behandlingId",
                status = HttpStatusCode.NotFound,
            )
        val meldekort = meldekortService.hentAlleMeldekortene(grunnlag.id)
        LOG.info(meldekort.toString())
        val dto =
            meldekort.map {
                MeldekortUtenDagerDTO(
                    id = it.id.toString(),
                    fom = it.fom,
                    tom = it.tom,
                    status = it.status.toString(),
                )
            }
        call.respond(status = HttpStatusCode.OK, dto)
    }

    post("$MELDEKORT_PATH/grunnlag") {
        innloggetBrukerProvider.krevSystembruker(call)
        val dto = call.receive<MeldekortGrunnlagDTO>()
        LOG.info { "Vi fikk nytt grunnlag : $dto" }

        meldekortService.mottaGrunnlag(mapGrunnlag(dto))
        LOG.info { "Persistert grunnlag for sakId ${dto.sakId}, returnerer 200 OK." }
        call.respond(message = true, status = HttpStatusCode.OK)
    }

    post("$MELDEKORT_PATH/godkjenn/{meldekortId}") {
        LOG.info { "Motatt request på $MELDEKORT_PATH/godkjenn/{meldekortId}" }
        val saksbehandler: Saksbehandler = innloggetBrukerProvider.krevInnloggetSaksbehandler(call)
        val meldekortId =
            call.parameters["meldekortId"]?.let { UUID.fromString(it) }
                ?: return@post call.respond(message = "meldekortId mangler", status = HttpStatusCode.NotFound)

        LOG.info { "Meldekort med id $meldekortId skal godkjenns" }
        meldekortService.godkjennMeldekort(meldekortId, saksbehandler.navIdent)

        call.respond(message = "OK", status = HttpStatusCode.OK)
    }
}

private fun mapGrunnlag(dto: MeldekortGrunnlagDTO): MeldekortGrunnlag =
    MeldekortGrunnlag(
        id = UUID.randomUUID(),
        sakId = dto.sakId,
        vedtakId = dto.vedtakId,
        behandlingId = dto.behandlingId,
        status =
        when (dto.status) {
            StatusDTO.AKTIV -> Status.AKTIV
            StatusDTO.IKKE_AKTIV -> Status.IKKE_AKTIV
        },
        vurderingsperiode =
        Periode(
            fra = dto.vurderingsperiode.fra,
            til = dto.vurderingsperiode.til,
        ),
        tiltak =
        dto.tiltak.map {
            Tiltak(
                id = UUID.randomUUID(),
                periode =
                Periode(
                    fra = it.periodeDTO.fra,
                    til = it.periodeDTO.til,
                ),
                tiltakstype = TiltakstypeSomGirRett.valueOf(it.typeKode),
                antDagerIUken = it.antDagerIUken,
            )
        },
        personopplysninger =
        Personopplysninger(
            fornavn = dto.personopplysninger.fornavn,
            etternavn = dto.personopplysninger.etternavn,
            ident = dto.personopplysninger.ident,
        ),
        utfallsperioder =
        dto.utfallsperioder.map {
            Utfallsperiode(
                fom = LocalDate.parse(it.fom),
                tom = LocalDate.parse(it.tom),
                utfall =
                when (it.utfall) {
                    UtfallForPeriodeDTO.GIR_RETT_TILTAKSPENGER -> UtfallForPeriode.GIR_RETT_TILTAKSPENGER
                    UtfallForPeriodeDTO.GIR_IKKE_RETT_TILTAKSPENGER -> UtfallForPeriode.GIR_IKKE_RETT_TILTAKSPENGER
                },
            )
        },
    )
