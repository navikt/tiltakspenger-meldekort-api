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
 * All logikk rundt *når* ekstern varsling (SMS/e-post via Altinn) skal leveres til bruker
 * ligger i domenet. Klienten oversetter kun domenets eksterne varslingstidspunkt fra
 * [LocalDateTime] til [ZonedDateTime] og sender det videre som Min side' `utsettSendingTil`.
 *
 * @link https://navikt.github.io/tms-dokumentasjon/varsler/
 */
class TmsVarselClientImpl(
    private val kafkaProducer: Producer<String, String>,
    private val topicName: String,
    private val meldekortFrontendUrl: String,
) : VarselClient {
    private val logger = KotlinLogging.logger {}

    /**
     * Bygger oppgave for nytt meldekort (uten Kafka-publisering). Trygg å kalle utenfor transaksjon.
     */
    override fun byggNyttMeldekortVarsel(
        varselId: VarselId,
        fnr: Fnr,
        utsettSendingTil: LocalDateTime?,
    ): SendtVarselMetadata {
        val varselHendelse: String = opprettNyttMeldekortOppgave(
            varselId = varselId,
            fnr = fnr,
            utsettSendingTil = utsettSendingTil?.atZone(zoneIdOslo),
        )
        return SendtVarselMetadata(jsonRequest = varselHendelse)
    }

    override fun byggMeldeperiodeEndretBeskjed(
        varselId: VarselId,
        fnr: Fnr,
        utsettSendingTil: LocalDateTime?,
    ): SendtVarselMetadata {
        val varselHendelse: String = opprettMeldeperiodeEndretBeskjed(
            varselId = varselId,
            fnr = fnr,
            utsettSendingTil = utsettSendingTil?.atZone(zoneIdOslo),
        )
        return SendtVarselMetadata(jsonRequest = varselHendelse)
    }

    /**
     * Publiserer en allerede bygd varselhendelse på Kafka.
     * Dersom `utsettSendingTil` (i payloaden) er null eller tilbake i tid sender Min side
     * eksternt varsel til Altinn umiddelbart.
     */
    override fun sendVarsel(varselId: VarselId, metadata: SendtVarselMetadata) {
        logger.info { "Sender varsel med id $varselId" }
        kafkaProducer.produce(topicName, varselId.toString(), metadata.jsonRequest)
    }

    /**
     * Inaktiverer varsel for bruker
     */
    override fun inaktiverVarsel(varselId: VarselId) {
        logger.info { "Inaktiverer varsel $varselId" }
        val inaktiveringHendelse = inaktiverVarselOppgave(varselId)

        kafkaProducer.produce(topicName, varselId.toString(), inaktiveringHendelse)
    }

    private fun opprettNyttMeldekortOppgave(
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

    private fun opprettMeldeperiodeEndretBeskjed(
        varselId: VarselId,
        fnr: Fnr,
        utsettSendingTil: ZonedDateTime?,
    ): String {
        return VarselActionBuilder.opprett {
            this.type = Varseltype.Beskjed
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
                tekst = "Vedtaket ditt om tiltakspenger er endret. Du må selv endre meldekortet for å få riktig utbetaling når endringen gjelder for en periode du allerede har sendt inn meldekort for.",
            )
        }
    }

    private fun inaktiverVarselOppgave(varselId: VarselId): String {
        return VarselActionBuilder.inaktiver {
            this.varselId = varselId.toString()
        }
    }
}
