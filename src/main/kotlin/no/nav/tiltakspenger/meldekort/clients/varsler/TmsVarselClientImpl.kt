package no.nav.tiltakspenger.meldekort.clients.varsler

import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class TmsVarselClientImpl(
    val kafkaProducer: KafkaProducer<String, String>,
    val topicName: String,
    val meldekortFrontendUrl: String,
) : TmsVarselClient {
    val logger = KotlinLogging.logger {}

    override fun sendVarselForNyttMeldekort(meldekort: BrukersMeldekort, eventId: String) {
        logger.info { "Sender varsel for meldekort ${meldekort.id} - event id: $eventId" }
        val varselHendelse = opprettVarselHendelse(meldekort, eventId)

        try {
            kafkaProducer.send(
                ProducerRecord(topicName, eventId, varselHendelse),
            )
        } catch (e: Exception) {
            logger.error(e) { "Feil ved sending av brukervarsel $eventId - ${e.message}" }
        }
    }

    // TODO: send som oppgave istedenfor beskjed, og send inaktiv-event når bruker har sendt inn meldekortet
    private fun opprettVarselHendelse(meldekort: BrukersMeldekort, eventId: String): String {
        return VarselActionBuilder.opprett {
            type = Varseltype.Beskjed
            varselId = eventId
            ident = meldekort.fnr.verdi
            sensitivitet = Sensitivitet.Substantial
            link = meldekortFrontendUrl
            tekster += Tekst(
                spraakkode = "nb",
                default = true,
                tekst = "Du har et meldekort klart til utfylling for ${meldekort.periode.fraOgMed} - ${meldekort.periode.tilOgMed}",
            )
        }
    }
}
