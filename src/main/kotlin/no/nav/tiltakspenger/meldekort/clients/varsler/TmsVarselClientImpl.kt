package no.nav.tiltakspenger.meldekort.clients.varsler

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder

/**
 * Tjeneste for å sende varsel til bruker
 * @link https://navikt.github.io/tms-dokumentasjon/varsler/
 */
class TmsVarselClientImpl(
    val kafkaProducer: Producer<String, String>,
    val topicName: String,
    val meldekortFrontendUrl: String,
) : TmsVarselClient {
    val logger = KotlinLogging.logger {}

    override fun sendVarselForNyttMeldekort(meldekort: Meldekort, varselId: String) {
        logger.info { "Sender varsel for meldekort ${meldekort.id} - varselId: $varselId" }
        val varselHendelse = opprettVarselOppgave(meldekort, varselId)

        try {
            kafkaProducer.produce(topicName, varselId, varselHendelse)
        } catch (e: Exception) {
            logger.error(e) { "Feil ved sending av brukervarsel $varselId - ${e.message}" }
        }
    }

    /**
     * Inaktiverer varsel for bruker, returnerer true eller false basert på om inaktiveringen ble lagt på kafka-topicet
     */
    override fun inaktiverVarsel(varselId: VarselId): Boolean {
        logger.info { "Inaktiverer varsel $varselId" }
        val inaktiveringHendelse = inaktiverVarselOppgave(varselId)

        try {
            kafkaProducer.produce(topicName, varselId.toString(), inaktiveringHendelse)
            return true
        } catch (e: Exception) {
            logger.error(e) { "Feil ved inaktivering av brukervarsel $varselId - ${e.message}" }
            return false
        }
    }

    private fun opprettVarselOppgave(meldekort: Meldekort, varselId: String): String {
        return VarselActionBuilder.opprett {
            this.type = Varseltype.Oppgave
            this.varselId = varselId
            this.ident = meldekort.fnr.verdi
            this.sensitivitet = Sensitivitet.Substantial
            this.link = meldekortFrontendUrl
            this.tekster += Tekst(
                spraakkode = "nb",
                default = true,
                tekst = "Du har et meldekort klart til utfylling for ${meldekort.periode.fraOgMed} - ${meldekort.periode.tilOgMed}",
            )
        }
    }

    private fun inaktiverVarselOppgave(varselId: VarselId): String {
        return VarselActionBuilder.inaktiver {
            this.varselId = varselId.toString()
        }
    }
}
