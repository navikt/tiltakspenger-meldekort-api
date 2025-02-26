package no.nav.tiltakspenger.meldekort.clients.varsler

import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.VarselId

class TmsVarselClientFake : TmsVarselClient {
    private val logger = KotlinLogging.logger {}

    override fun sendVarselForNyttMeldekort(meldekort: Meldekort, varselId: String) {
        logger.info { "Sender (ikke) event $varselId for meldekort ${meldekort.id}" }
    }

    override fun inaktiverVarsel(varselId: VarselId): Boolean {
        logger.info { "Inaktiverer (ikke) varsel $varselId" }
        return true
    }
}
