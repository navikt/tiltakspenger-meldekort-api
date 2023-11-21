package no.nav.tiltakspenger.meldekort.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.dto.MeldekortDTO

private val LOG = KotlinLogging.logger{}

internal const val MELDEKORT_PATH = "/meldekort"
fun Route.meldekort() {
    post(MELDEKORT_PATH) {
        LOG.info("Motatt request på $MELDEKORT_PATH")
        val nyttMeldekort = call.receive<MeldekortDTO>()

    }

}
