package no.nav.tiltakspenger.meldekort.identhendelse.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.FnrGenerator
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.json.serialize
import org.junit.jupiter.api.Test
import java.util.UUID

class IdenthendelseDtoTest {
    private val fnrGenerator = FnrGenerator()

    @Test
    fun `serialiserer og deserialiserer identhendelse dto`() {
        val gammeltFnr = fnrGenerator.generer().verdi
        val nyttFnr = fnrGenerator.generer().verdi
        val identhendelseDto = IdenthendelseDto(
            gammeltFnr = gammeltFnr,
            nyttFnr = nyttFnr,
        )
        val forventetJson = """
            {
              "gammeltFnr": "$gammeltFnr",
              "nyttFnr": "$nyttFnr"
            }
        """.trimIndent()

        val serialisert = serialize(identhendelseDto)

        objectMapper.readTree(serialisert) shouldBe objectMapper.readTree(forventetJson)
        objectMapper.readValue(serialisert, IdenthendelseDto::class.java) shouldBe identhendelseDto
    }

    @Test
    fun `mapper dto til domene`() {
        val id = UUID.randomUUID()
        val gammeltFnr = fnrGenerator.generer()
        val nyttFnr = fnrGenerator.generer()
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
