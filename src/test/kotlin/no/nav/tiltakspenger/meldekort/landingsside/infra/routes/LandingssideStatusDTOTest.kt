package no.nav.tiltakspenger.meldekort.landingsside.infra.routes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideMeldekort
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideStatus
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LandingssideStatusDTOTest {
    @Test
    fun `serialiserer og deserialiserer landingsside status response`() {
        val landingssideStatusDTO = LandingssideStatusDTO(
            harInnsendteMeldekort = true,
            meldekortTilUtfylling = listOf(
                LandingssideStatusDTO.LandingssideMeldekortDTO(
                    kanSendesFra = LocalDateTime.parse("2025-01-17T15:00:00"),
                ),
            ),
            redirectUrl = "https://www.nav.no/tiltakspenger/meldekort",
        )
        val forventetJson = """
            {
              "harInnsendteMeldekort": true,
              "meldekortTilUtfylling": [
                {
                  "kanSendesFra": "2025-01-17T15:00:00",
                  "kanFyllesUtFra": "2025-01-17T15:00:00",
                  "fristForInnsending": null
                }
              ],
              "redirectUrl": "https://www.nav.no/tiltakspenger/meldekort"
            }
        """.trimIndent()

        val serialisert = serialize(landingssideStatusDTO)

        objectMapper.readTree(serialisert) shouldBe objectMapper.readTree(forventetJson)
        objectMapper.readValue(serialisert, LandingssideStatusDTO::class.java) shouldBe landingssideStatusDTO
    }

    @Test
    fun `mapper domene status til route dto`() {
        val landingssideStatus = LandingssideStatus(
            harInnsendteMeldekort = false,
            meldekortTilUtfylling = listOf(
                LandingssideMeldekort(kanSendesFra = LocalDateTime.parse("2025-01-31T15:00:00")),
            ),
            redirectUrl = "https://www.nav.no/tiltakspenger/meldekort",
        )

        landingssideStatus.tilLandingssideStatusDTO() shouldBe LandingssideStatusDTO(
            harInnsendteMeldekort = false,
            meldekortTilUtfylling = listOf(
                LandingssideStatusDTO.LandingssideMeldekortDTO(
                    kanSendesFra = LocalDateTime.parse("2025-01-31T15:00:00"),
                ),
            ),
            redirectUrl = "https://www.nav.no/tiltakspenger/meldekort",
        )
    }
}
