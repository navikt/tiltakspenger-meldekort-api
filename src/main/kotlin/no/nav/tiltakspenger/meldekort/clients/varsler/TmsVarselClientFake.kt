package no.nav.tiltakspenger.meldekort.clients.varsler

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.VarselId

class TmsVarselClientFake : TmsVarselClient {
    private val logger = KotlinLogging.logger {}

    override fun sendVarselForNyttMeldekort(meldekort: Meldekort, varselId: VarselId): SendtVarselMetadata {
        logger.info { "Sender (ikke) event $varselId for meldekort ${meldekort.id}" }
        // Ikke likt innhold som i Nais (vi bare lagrer den i basen)
        return SendtVarselMetadata(""""json-request"""")
    }

    override fun inaktiverVarsel(varselId: VarselId) {
        logger.info { "Inaktiverer (ikke) varsel $varselId" }
    }
}
