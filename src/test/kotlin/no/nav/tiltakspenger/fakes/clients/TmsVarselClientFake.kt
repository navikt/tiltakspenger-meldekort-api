package no.nav.tiltakspenger.fakes.clients

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.clients.varsler.SendtVarselMetadata
import no.nav.tiltakspenger.meldekort.clients.varsler.VarselClient
import no.nav.tiltakspenger.meldekort.domene.VarselId
import java.time.LocalDateTime

class TmsVarselClientFake : VarselClient {
    private val logger = KotlinLogging.logger {}
    private val sendteVarsler = mutableListOf<SendtVarsel>()
    private val inaktiverteVarsler = mutableListOf<VarselId>()

    // Mellomlagrer fnr/utsettSendingTil pr varselId mellom bygg* og sendVarsel,
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

    override fun byggNyttMeldekortVarsel(
        varselId: VarselId,
        fnr: Fnr,
        utsettSendingTil: LocalDateTime?,
    ): SendtVarselMetadata = byggVarsel(
        varselId = varselId,
        fnr = fnr,
        utsettSendingTil = utsettSendingTil,
        type = Varseltype.NyttMeldekort,
    )

    override fun byggMeldeperiodeEndretBeskjed(
        varselId: VarselId,
        fnr: Fnr,
        utsettSendingTil: LocalDateTime?,
    ): SendtVarselMetadata = byggVarsel(
        varselId = varselId,
        fnr = fnr,
        utsettSendingTil = utsettSendingTil,
        type = Varseltype.MeldeperiodeEndret,
    )

    private fun byggVarsel(
        varselId: VarselId,
        fnr: Fnr,
        utsettSendingTil: LocalDateTime?,
        type: Varseltype,
    ): SendtVarselMetadata {
        logger.info { "Fake: Bygger varsel med id $varselId (type=$type, utsettSendingTil=$utsettSendingTil)" }
        byggetMenIkkeSendt[varselId] = ByggetVarsel(fnr = fnr, utsettSendingTil = utsettSendingTil, type = type)
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
                type = bygget.type,
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
        val type: Varseltype,
    )

    data class SendtVarsel(
        val varselId: VarselId,
        val fnr: Fnr,
        val utsettSendingTil: LocalDateTime?,
        val type: Varseltype,
    )

    enum class Varseltype {
        NyttMeldekort,
        MeldeperiodeEndret,
    }

    data class Varselhendelser(
        val sendteVarsler: List<SendtVarsel>,
        val inaktiverteVarsler: List<VarselId>,
    )
}
