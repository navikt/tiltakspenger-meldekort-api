package no.nav.tiltakspenger.meldekort.api.routes

import io.ktor.server.application.call
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDTO
import no.nav.tiltakspenger.meldekort.api.repository.MeldekortRepoImpl
import no.nav.tiltakspenger.meldekort.api.service.MeldekortServiceImpl

private val LOG = KotlinLogging.logger {}

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
        if (meldekortIdent != null) {
            return@get meldekortService.hentMeldekort(meldekortIdent)
        } else {
            throw NotFoundException("Meldekort med ident:$meldekortIdent eksisterer ikke i databasen")
        }
    }
    get("$MELDEKORT_PATH/hentAlle/{behandlingsId/sakId}") {
        LOG.info("Motatt request på $MELDEKORT_PATH/hentAlle/{behandlingsId/sakId}")
    }
}
