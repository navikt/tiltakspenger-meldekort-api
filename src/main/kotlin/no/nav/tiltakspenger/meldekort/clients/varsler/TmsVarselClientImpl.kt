package no.nav.tiltakspenger.meldekort.clients.varsler

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import java.time.LocalDateTime
import java.time.ZonedDateTime

/**
 * Tjeneste for å sende varsel til bruker.
 *
 * All logikk rundt *når* varselet skal leveres til bruker ligger i domenet (typisk
 * `Varsel.SkalAktiveresTidspunkt`). Klienten oversetter kun fra [LocalDateTime] til [ZonedDateTime]
 * og lener seg på Min side' `utsettSendingTil` for utsatt levering.
 *
 * @link https://navikt.github.io/tms-dokumentasjon/varsler/
 */
class TmsVarselClientImpl(
    private val kafkaProducer: Producer<String, String>,
    private val topicName: String,
    private val meldekortFrontendUrl: String,
) : VarselClient {
    private val logger = KotlinLogging.logger {}

    override fun sendVarsel(
        varselId: VarselId,
        fnr: Fnr,
        utsettSendingTil: LocalDateTime?,
    ): SendtVarselMetadata {
        logger.info { "Sender varsel med id $varselId (utsettSendingTil=$utsettSendingTil)" }
        val varselHendelse: String = opprettVarselOppgave(
            varselId = varselId,
            fnr = fnr,
            utsettSendingTil = utsettSendingTil?.atZone(zoneIdOslo),
        )
        val sendtVarselMetadata = SendtVarselMetadata(jsonRequest = varselHendelse)
        kafkaProducer.produce(topicName, varselId.toString(), varselHendelse)
        return sendtVarselMetadata
    }

    /**
     * Inaktiverer varsel for bruker, returnerer true eller false basert på om inaktiveringen ble lagt på kafka-topicet
     */
    override fun inaktiverVarsel(varselId: VarselId) {
        logger.info { "Inaktiverer varsel $varselId" }
        val inaktiveringHendelse = inaktiverVarselOppgave(varselId)

        kafkaProducer.produce(topicName, varselId.toString(), inaktiveringHendelse)
    }

    private fun opprettVarselOppgave(
        varselId: VarselId,
        fnr: Fnr,
        utsettSendingTil: ZonedDateTime?,
    ): String {
        return VarselActionBuilder.opprett {
            this.type = Varseltype.Oppgave
            this.varselId = varselId.toString()
            this.ident = fnr.verdi
            this.sensitivitet = Sensitivitet.Substantial
            this.link = meldekortFrontendUrl
            this.eksternVarsling {
                preferertKanal = EksternKanal.SMS
                this.utsettSendingTil = utsettSendingTil
            }
            this.tekster += Tekst(
                spraakkode = "nb",
                default = true,
                tekst = "Du har fått et nytt meldekort for tiltakspenger. Du må fylle ut og sende inn meldekortet før du kan få tiltakspengene dine.",
            )
        }
    }

    private fun inaktiverVarselOppgave(varselId: VarselId): String {
        return VarselActionBuilder.inaktiver {
            this.varselId = varselId.toString()
        }
    }
}
