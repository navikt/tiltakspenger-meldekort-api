package no.nav.tiltakspenger.meldekort.clients.varsler

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class TmsVarselClientImpl(val kafkaProducer: KafkaProducer<String, String>, val topicName: String) : TmsVarselClient {
    val logger = KotlinLogging.logger {}

    override fun sendVarselForNyttMeldekort(meldekort: BrukersMeldekort, eventId: String) {
        logger.info { "Sender varsel for meldekort ${meldekort.id} - event id: $eventId" }
        val varselHendelse = opprettVarselHendelse(eventId, meldekort.fnr)

        try {
            kafkaProducer.send(
                ProducerRecord(topicName, eventId, varselHendelse),
            )
        } catch (e: Exception) {
            logger.error(e) { "Feil ved sending av brukervarsel $eventId - ${e.message}" }
        }
    }

    private fun opprettVarselHendelse(eventId: String, fnr: Fnr): String {
        return VarselActionBuilder.opprett {
            type = Varseltype.Beskjed
            varselId = eventId
            ident = fnr.verdi
            tekster += Tekst(spraakkode = "nb", tekst = "Test meldekort varsel", default = true)
            sensitivitet = Sensitivitet.Substantial
        }
    }
}
