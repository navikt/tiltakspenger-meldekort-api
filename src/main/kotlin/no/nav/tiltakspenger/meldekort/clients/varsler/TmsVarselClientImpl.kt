package no.nav.tiltakspenger.meldekort.clients.varsler

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tms.varsel.action.EksternKanal
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

    override fun sendVarselForNyttMeldekort(meldekort: Meldekort, varselId: VarselId) {
        logger.info { "Sender varsel for meldekort ${meldekort.id} - varselId: $varselId" }
        val varselHendelse = opprettVarselOppgave(meldekort, varselId)

        kafkaProducer.produce(topicName, varselId.toString(), varselHendelse)
    }

    /**
     * Inaktiverer varsel for bruker, returnerer true eller false basert på om inaktiveringen ble lagt på kafka-topicet
     */
    override fun inaktiverVarsel(varselId: VarselId) {
        logger.info { "Inaktiverer varsel $varselId" }
        val inaktiveringHendelse = inaktiverVarselOppgave(varselId)

        kafkaProducer.produce(topicName, varselId.toString(), inaktiveringHendelse)
    }

    private fun opprettVarselOppgave(meldekort: Meldekort, varselId: VarselId): String {
        return VarselActionBuilder.opprett {
            this.type = Varseltype.Oppgave
            this.varselId = varselId.toString()
            this.ident = meldekort.fnr.verdi
            this.sensitivitet = Sensitivitet.High
            this.link = meldekortFrontendUrl
            this.eksternVarsling {
                preferertKanal = EksternKanal.SMS
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
