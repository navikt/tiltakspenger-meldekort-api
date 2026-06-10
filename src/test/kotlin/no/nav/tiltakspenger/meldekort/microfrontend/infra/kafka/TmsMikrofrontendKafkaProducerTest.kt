package no.nav.tiltakspenger.meldekort.microfrontend.infra.kafka

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import org.junit.jupiter.api.Test

class TmsMikrofrontendKafkaProducerTest {
    private val topic = "test-topic"

    /** Ekte fake av Kafka-sømmen – fanger produserte meldinger uten mock. */
    private class ProdusertMeldingFake {
        data class Melding(val topic: String, val key: String, val value: String)

        val meldinger = mutableListOf<Melding>()

        fun produser(topic: String, key: String, value: String) {
            meldinger.add(Melding(topic, key, value))
        }
    }

    @Test
    fun `aktiver produserer enable-melding med sakId som key`() {
        val fnr = Fnr.random()
        val sakId = SakId.random()
        val fake = ProdusertMeldingFake()
        val producer = TmsMikrofrontendKafkaProducer(topic, fake::produser)

        producer.aktiverMicrofrontendForBruker(fnr, sakId)

        fake.meldinger.size shouldBe 1
        val melding = fake.meldinger.single()
        melding.topic shouldBe topic
        melding.key shouldBe sakId.toString()
        melding.value shouldContain MICROFRONTEND_APP
        melding.value shouldContain fnr.verdi
    }

    @Test
    fun `inaktiver produserer disable-melding med sakId som key`() {
        val fnr = Fnr.random()
        val sakId = SakId.random()
        val fake = ProdusertMeldingFake()
        val producer = TmsMikrofrontendKafkaProducer(topic, fake::produser)

        producer.inaktiverMicrofrontendForBruker(fnr, sakId)

        fake.meldinger.size shouldBe 1
        val melding = fake.meldinger.single()
        melding.topic shouldBe topic
        melding.key shouldBe sakId.toString()
        melding.value shouldContain MICROFRONTEND_APP
    }

    @Test
    fun `feil under produsering gir KafkaFeil`() {
        val producer = TmsMikrofrontendKafkaProducer(topic) { _, _, _ ->
            throw RuntimeException("simulert kafka-feil")
        }

        producer.aktiverMicrofrontendForBruker(Fnr.random(), SakId.random()).isLeft() shouldBe true
        producer.inaktiverMicrofrontendForBruker(Fnr.random(), SakId.random()).isLeft() shouldBe true
    }
}
