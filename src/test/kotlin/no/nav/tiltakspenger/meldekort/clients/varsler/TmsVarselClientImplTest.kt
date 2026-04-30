package no.nav.tiltakspenger.meldekort.clients.varsler

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tms.varsel.builder.BuilderEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TmsVarselClientImplTest {
    private val fnr = Fnr.fromString("12345678911")
    private val zoneId = ZoneId.of("Europe/Oslo")

    @BeforeEach
    fun setUp() {
        BuilderEnvironment.extend(
            mapOf(
                "NAIS_CLUSTER_NAME" to "dev-gcp",
                "NAIS_NAMESPACE" to "tiltakspenger",
                "NAIS_APP_NAME" to "tiltakspenger-meldekort-api",
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        BuilderEnvironment.reset()
    }

    @Test
    fun `setter ikke utsettSendingTil når null sendes inn`() {
        val payloads = mutableListOf<String>()
        val client = nyClient(payloads)

        val metadata = client.sendVarsel(VarselId.random(), fnr, utsettSendingTil = null)

        payloads shouldBe listOf(metadata.jsonRequest)
        metadata.jsonRequest.shouldNotContain("utsettSendingTil")
    }

    @Test
    fun `setter utsettSendingTil med Oslo-tidssone når tidspunkt er gitt`() {
        val payloads = mutableListOf<String>()
        val client = nyClient(payloads)
        val tidspunkt = LocalDateTime.of(2025, 3, 10, 9, 0)
        val forventet = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(tidspunkt.atZone(zoneId))

        val metadata = client.sendVarsel(VarselId.random(), fnr, utsettSendingTil = tidspunkt)

        payloads shouldBe listOf(metadata.jsonRequest)
        metadata.jsonRequest.shouldContain("\"utsettSendingTil\":\"$forventet\"")
    }

    private fun nyClient(payloads: MutableList<String>): TmsVarselClientImpl {
        val kafkaProducer = mockk<Producer<String, String>>()
        every { kafkaProducer.produce(any(), any(), capture(payloads)) } just runs

        return TmsVarselClientImpl(
            kafkaProducer = kafkaProducer,
            topicName = "topic",
            meldekortFrontendUrl = "https://www.nav.no/meldekort",
        )
    }
}
