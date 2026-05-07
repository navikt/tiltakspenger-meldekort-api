package no.nav.tiltakspenger.meldekort.identhendelse.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.json.serialize
import org.junit.jupiter.api.Test
import java.util.UUID

class IdenthendelseDtoTest {
    @Test
    fun `serialiserer og deserialiserer identhendelse dto`() {
        val identhendelseDto = IdenthendelseDto(
            gammeltFnr = "12345678910",
            nyttFnr = "10987654321",
        )
        val forventetJson = """
            {
              "gammeltFnr": "12345678910",
              "nyttFnr": "10987654321"
            }
        """.trimIndent()

        val serialisert = serialize(identhendelseDto)

        objectMapper.readTree(serialisert) shouldBe objectMapper.readTree(forventetJson)
        objectMapper.readValue(serialisert, IdenthendelseDto::class.java) shouldBe identhendelseDto
    }

    @Test
    fun `mapper dto til domene`() {
        val id = UUID.randomUUID()
        val gammeltFnr = Fnr.random()
        val nyttFnr = Fnr.random()
        val identhendelseDto = IdenthendelseDto(
            gammeltFnr = gammeltFnr.verdi,
            nyttFnr = nyttFnr.verdi,
        )

        val identhendelse = identhendelseDto.tilIdenthendelse(id)

        identhendelse.id shouldBe id
        identhendelse.gammeltFnr shouldBe gammeltFnr
        identhendelse.nyttFnr shouldBe nyttFnr
    }
}
