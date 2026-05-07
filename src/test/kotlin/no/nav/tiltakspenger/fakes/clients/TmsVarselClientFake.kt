package no.nav.tiltakspenger.fakes.clients

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.varsler.SendtVarselMetadata
import no.nav.tiltakspenger.meldekort.varsler.VarselClient
import no.nav.tiltakspenger.meldekort.varsler.VarselId
import java.time.LocalDateTime

class TmsVarselClientFake : VarselClient {
    private val logger = KotlinLogging.logger {}
    private val sendteVarsler = mutableListOf<SendtVarsel>()
    private val inaktiverteVarsler = mutableListOf<VarselId>()

    // Mellomlagrer fnr/utsettSendingTil pr varselId mellom byggVarsel og sendVarsel,
    // slik at vi kan rapportere disse i [SendtVarsel] når sendVarsel faktisk publiserer.
    private val byggetMenIkkeSendt = mutableMapOf<VarselId, ByggetVarsel>()

    fun hentSendteVarsler(): List<SendtVarsel> = sendteVarsler.toList()

    fun hentInaktiverteVarsler(): List<VarselId> = inaktiverteVarsler.toList()

    fun snapshotVarselhendelser(): Varselhendelser {
        return Varselhendelser(
            sendteVarsler = hentSendteVarsler(),
            inaktiverteVarsler = hentInaktiverteVarsler(),
        )
    }

    override fun byggVarsel(
        varselId: VarselId,
        fnr: Fnr,
        utsettSendingTil: LocalDateTime?,
    ): SendtVarselMetadata {
        logger.info { "Fake: Bygger varsel med id $varselId (utsettSendingTil=$utsettSendingTil)" }
        byggetMenIkkeSendt[varselId] = ByggetVarsel(fnr = fnr, utsettSendingTil = utsettSendingTil)
        // Ikke likt innhold som i Nais (vi bare lagrer den i basen)
        return SendtVarselMetadata(""""json-request"""")
    }

    override fun sendVarsel(varselId: VarselId, metadata: SendtVarselMetadata) {
        logger.info { "Fake: Sender (ikke) varsel med id $varselId" }
        val bygget = byggetMenIkkeSendt.remove(varselId)
            ?: error("Fake: sendVarsel($varselId) kalt uten foregående byggVarsel")
        sendteVarsler.add(
            SendtVarsel(
                varselId = varselId,
                fnr = bygget.fnr,
                utsettSendingTil = bygget.utsettSendingTil,
            ),
        )
    }

    override fun inaktiverVarsel(varselId: VarselId) {
        logger.info { "Inaktiverer (ikke) varsel $varselId" }
        inaktiverteVarsler.add(varselId)
    }

    private data class ByggetVarsel(
        val fnr: Fnr,
        val utsettSendingTil: LocalDateTime?,
    )

    data class SendtVarsel(
        val varselId: VarselId,
        val fnr: Fnr,
        val utsettSendingTil: LocalDateTime?,
    )

    data class Varselhendelser(
        val sendteVarsler: List<SendtVarsel>,
        val inaktiverteVarsler: List<VarselId>,
    )
}
