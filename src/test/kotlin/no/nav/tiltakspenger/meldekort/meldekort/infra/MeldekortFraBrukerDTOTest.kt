package no.nav.tiltakspenger.meldekort.meldekort.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDag
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortFraBrukerDTOTest {
    @Test
    fun `serialiserer og deserialiserer meldekort fra bruker request`() {
        val meldekortId = MeldekortId.random().toString()
        val meldekortFraBrukerDTO = MeldekortFraBrukerDTO(
            id = meldekortId,
            dager = listOf(
                MeldekortDagFraBrukerDTO(
                    dag = LocalDate.parse("2025-01-06"),
                    status = MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET,
                ),
                MeldekortDagFraBrukerDTO(
                    dag = LocalDate.parse("2025-01-07"),
                    status = MeldekortDagStatusDTO.FRAVÆR_SYK,
                ),
            ),
            locale = "nb",
        )
        val forventetJson = """
            {
              "id": "$meldekortId",
              "dager": [
                {
                  "dag": "2025-01-06",
                  "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                },
                {
                  "dag": "2025-01-07",
                  "status": "FRAVÆR_SYK"
                }
              ],
              "locale": "nb"
            }
        """.trimIndent()

        val serialisert = serialize(meldekortFraBrukerDTO)

        objectMapper.readTree(serialisert) shouldBe objectMapper.readTree(forventetJson)
        objectMapper.readValue(serialisert, MeldekortFraBrukerDTO::class.java) shouldBe meldekortFraBrukerDTO
    }

    @Test
    fun `mapper request dto til domene kommando`() {
        val fnr = Fnr.fromString("12345678910")
        val meldekortId = MeldekortId.random()
        val meldekortFraBrukerDTO = MeldekortFraBrukerDTO(
            id = meldekortId.toString(),
            dager = listOf(
                MeldekortDagFraBrukerDTO(
                    dag = LocalDate.parse("2025-01-06"),
                    status = MeldekortDagStatusDTO.DELTATT_MED_LØNN_I_TILTAKET,
                ),
            ),
            locale = null,
        )

        val kommando = meldekortFraBrukerDTO.tilLagreKommando(fnr)

        kommando.id shouldBe meldekortId
        kommando.fnr shouldBe fnr
        kommando.dager shouldBe listOf(
            MeldekortDag(
                dag = LocalDate.parse("2025-01-06"),
                status = MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET,
            ),
        )
        kommando.locale shouldBe null
    }
}
