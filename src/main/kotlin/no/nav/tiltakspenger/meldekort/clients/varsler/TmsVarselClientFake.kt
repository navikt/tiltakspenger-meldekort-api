package no.nav.tiltakspenger.meldekort.clients.varsler

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.VarselId

class TmsVarselClientFake : TmsVarselClient {
    private val logger = KotlinLogging.logger {}
    private val sendteVarsler = mutableListOf<SendtVarsel>()
    private val inaktiverteVarsler = mutableListOf<VarselId>()

    fun hentSendteVarsler(): List<SendtVarsel> = sendteVarsler.toList()

    fun hentInaktiverteVarsler(): List<VarselId> = inaktiverteVarsler.toList()

    fun snapshotVarselhendelser(): Varselhendelser {
        return Varselhendelser(
            sendteVarsler = hentSendteVarsler(),
            inaktiverteVarsler = hentInaktiverteVarsler(),
        )
    }

    override fun sendVarselForNyttMeldekort(meldekort: Meldekort, varselId: VarselId): SendtVarselMetadata {
        logger.info { "Sender (ikke) event $varselId for meldekort ${meldekort.id}" }
        sendteVarsler.add(
            SendtVarsel(
                meldekortId = meldekort.id,
                varselId = varselId,
            ),
        )
        // Ikke likt innhold som i Nais (vi bare lagrer den i basen)
        return SendtVarselMetadata(""""json-request"""")
    }

    override fun inaktiverVarsel(varselId: VarselId) {
        logger.info { "Inaktiverer (ikke) varsel $varselId" }
        inaktiverteVarsler.add(varselId)
    }

    data class SendtVarsel(
        val meldekortId: MeldekortId,
        val varselId: VarselId,
    )

    data class Varselhendelser(
        val sendteVarsler: List<SendtVarsel>,
        val inaktiverteVarsler: List<VarselId>,
    )
}
