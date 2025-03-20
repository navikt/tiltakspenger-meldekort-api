package no.nav.tiltakspenger.meldekort.clients.varsler

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.VarselId

class TmsVarselClientFake : TmsVarselClient {
    private val logger = KotlinLogging.logger {}

    override fun sendVarselForNyttMeldekort(meldekort: Meldekort, varselId: VarselId) {
        logger.info { "Sender (ikke) event $varselId for meldekort ${meldekort.id}" }
    }

    override fun inaktiverVarsel(varselId: VarselId) {
        logger.info { "Inaktiverer (ikke) varsel $varselId" }
    }
}
