package no.nav.tiltakspenger.meldekort.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.dto.MeldekortDTO

private val LOG = KotlinLogging.logger{}

internal const val MELDEKORT_PATH = "/meldekort"
fun Route.meldekort() {
    post("$MELDEKORT_PATH/opprett") {
        LOG.info("Motatt request på $MELDEKORT_PATH")
        val nyttMeldekort = call.receive<MeldekortDTO>()

    }
    get("$MELDEKORT_PATH/hent/{meldekortId}") {
        LOG.info("Motatt request på $MELDEKORT_PATH/hent/{meldekortId}")
        //val nyttMeldekort = call.receive<MeldekortDTO>()

    }
    get("$MELDEKORT_PATH/hentAlle/{behandlingsId/sakId}") {
        LOG.info("Motatt request på $MELDEKORT_PATH/hentAlle/{behandlingsId/sakId}")
        //val nyttMeldekort = call.receive<MeldekortDTO>()

    }
}
