package no.nav.tiltakspenger.meldekort.clients.varsler

import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.domene.Meldekort

class TmsVarselClientFake : TmsVarselClient {
    private val logger = KotlinLogging.logger {}

    override fun sendVarselForNyttMeldekort(meldekort: Meldekort, eventId: String) {
        logger.info { "Sender (ikke) event $eventId for meldekort ${meldekort.id}" }
    }
}
