package no.nav.tiltakspenger.meldekort.clients.varsler

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.domene.VarselId

class TmsVarselClientFake : VarselClient {
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

    override fun sendVarsel(varselId: VarselId, fnr: Fnr): SendtVarselMetadata {
        logger.info { "Fake: Sender (ikke) varsel med id $varselId" }
        sendteVarsler.add(
            SendtVarsel(
                varselId = varselId,
                fnr = fnr,
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
        val varselId: VarselId,
        val fnr: Fnr,
    )

    data class Varselhendelser(
        val sendteVarsler: List<SendtVarsel>,
        val inaktiverteVarsler: List<VarselId>,
    )
}
