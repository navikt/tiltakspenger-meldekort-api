package no.nav.tiltakspenger.meldekort.routes

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.dto.MeldekortDTO
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepoImpl
import no.nav.tiltakspenger.meldekort.service.MeldekortServiceImpl

private val LOG = KotlinLogging.logger{}

private const val MELDEKORT_PATH = "/meldekort"
fun Route.meldekort() {
    val meldekortService = MeldekortServiceImpl(MeldekortRepoImpl())

    post("$MELDEKORT_PATH/opprett") {
        LOG.info("Motatt request på $MELDEKORT_PATH")
        val meldekortDTO = call.receive<MeldekortDTO>()
        meldekortService.opprettMeldekort(meldekortDTO)
    }
    get("$MELDEKORT_PATH/hent/{meldekortId}") {
        LOG.info("Motatt request på $MELDEKORT_PATH/hent/{meldekortId}")
        val meldekortIdent = call.parameters["meldekortId"]
        if (meldekortIdent != null)
            return@get meldekortService.hentMeldekort(meldekortIdent)
        else
            throw NotFoundException("Meldekort med ident:$meldekortIdent eksisterer ikke i databasen")

    }
    get("$MELDEKORT_PATH/hentAlle/{behandlingsId/sakId}") {
        LOG.info("Motatt request på $MELDEKORT_PATH/hentAlle/{behandlingsId/sakId}")

    }
}
