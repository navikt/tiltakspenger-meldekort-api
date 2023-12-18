package no.nav.tiltakspenger.meldekort.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.api.dto.Meldekort
import no.nav.tiltakspenger.meldekort.api.service.MeldekortService
import java.time.DayOfWeek
import java.time.LocalDate

private val LOG = KotlinLogging.logger {}

private const val MELDEKORT_PATH = "/meldekort"
fun Route.meldekort(
    meldekortService: MeldekortService,
) {
    post("$MELDEKORT_PATH/opprett") {
        LOG.info("Motatt request på $MELDEKORT_PATH")
        val meldekortDTO = call.receive<Meldekort.Registrert>()
        meldekortService.opprettMeldekort(meldekortDTO)
    }
    get("$MELDEKORT_PATH/hent/{meldekortId}") {
        LOG.info("Motatt request på $MELDEKORT_PATH/hent/{meldekortId}")
        val meldekortIdent = call.parameters["meldekortId"]
        if (meldekortIdent != null) {
            call.respond(status = HttpStatusCode.OK, message = meldekortService.hentMeldekort(meldekortIdent))
        } else {
            throw NotFoundException("Meldekort med ident:$meldekortIdent eksisterer ikke i databasen")
        }
    }
    get("$MELDEKORT_PATH/hentAlle/{behandlingsId/sakId}") {
        LOG.info("Motatt request på $MELDEKORT_PATH/hentAlle/{behandlingsId/sakId}")
    }

    post("$MELDEKORT_PATH/nyDag") {
        LOG.info("Det er en ny dag")
        val nyDag = call.receive<DayHasBegunDTO>().date
        LOG.info("Den ny dagen er: $nyDag")
        if (nyDag.dayOfWeek == DayOfWeek.FRIDAY) {
            LOG.info { "Det er fredag!!! Kanskje vi skal generere meldekort her??" }
        }
        call.respond(message = "OK", status = HttpStatusCode.OK)
    }

    post("$MELDEKORT_PATH/grunnlag") {
        val dto = call.receive<MeldekortGrunnlagDTO>()

        LOG.info { "Vi fikk nytt grunnlag : $dto" }
        meldekortService.mottaGrunnlag(dto)
        call.respond(message = "OK", status = HttpStatusCode.OK)
    }
}

data class MeldekortGrunnlagDTO(
    val vedtakId: String,
    val behandlingId: String,
    val status: StatusDTO,
    val vurderingsperiode: PeriodeDTO,
    val tiltak: List<TiltakDTO>,
)

enum class StatusDTO {
    AKTIV,
    IKKE_AKTIV,
}

data class TiltakDTO(
    val periodeDTO: PeriodeDTO,
    val typeBeskrivelse: String,
    val typeKode: String,
    val antDagerIUken: Float,
)
data class PeriodeDTO(
    val fra: LocalDate,
    val til: LocalDate,
)

data class DayHasBegunDTO(val date: LocalDate)
