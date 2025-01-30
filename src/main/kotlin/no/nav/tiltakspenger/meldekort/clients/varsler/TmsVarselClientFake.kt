package no.nav.tiltakspenger.meldekort.clients.varsler

import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort

class TmsVarselClientFake : TmsVarselClient {
    private val logger = KotlinLogging.logger {}

    override fun sendVarselForNyttMeldekort(meldekort: BrukersMeldekort, eventId: String) {
        logger.info { "Sender (ikke) event $eventId for meldekort ${meldekort.id}" }
    }
}
