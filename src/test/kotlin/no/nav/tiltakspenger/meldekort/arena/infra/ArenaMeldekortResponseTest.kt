package no.nav.tiltakspenger.meldekort.arena.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekort
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArenaMeldekortResponseTest {
    @Test
    fun `serialiserer og deserialiserer arena meldekort response`() {
        val arenaMeldekortResponse = ArenaMeldekortResponse(
            personId = 123L,
            etternavn = "Nordmann",
            fornavn = "Ola",
            maalformkode = "NB",
            meldeform = "ELEKTRONISK",
            antallGjenstaaendeFeriedager = 2,
            meldekortListe = listOf(
                ArenaMeldekort(
                    meldekortId = 456L,
                    kortType = "ELEKTRONISK",
                    meldeperiode = "202501",
                    fraDato = LocalDate.parse("2025-01-06"),
                    tilDato = LocalDate.parse("2025-01-19"),
                    hoyesteMeldegruppe = "INDIV",
                    beregningstatus = "OPPRETTET",
                    forskudd = false,
                    mottattDato = null,
                    bruttoBelop = 0F,
                ),
            ),
            fravaerListe = null,
        )
        val forventetJson = """
            {
              "personId": 123,
              "etternavn": "Nordmann",
              "fornavn": "Ola",
              "maalformkode": "NB",
              "meldeform": "ELEKTRONISK",
              "antallGjenstaaendeFeriedager": 2,
              "meldekortListe": [
                {
                  "meldekortId": 456,
                  "kortType": "ELEKTRONISK",
                  "meldeperiode": "202501",
                  "fraDato": "2025-01-06",
                  "tilDato": "2025-01-19",
                  "hoyesteMeldegruppe": "INDIV",
                  "beregningstatus": "OPPRETTET",
                  "forskudd": false,
                  "mottattDato": null,
                  "bruttoBelop": 0.0
                }
              ],
              "fravaerListe": null
            }
        """.trimIndent()

        val serialisert = serialize(arenaMeldekortResponse)

        objectMapper.readTree(serialisert) shouldBe objectMapper.readTree(forventetJson)
        objectMapper.readValue(serialisert, ArenaMeldekortResponse::class.java) shouldBe arenaMeldekortResponse
    }

    @Test
    fun `mapper response dto til domene`() {
        val arenaMeldekortResponse = ArenaMeldekortResponse(
            personId = 123L,
            etternavn = "Nordmann",
            fornavn = "Ola",
            maalformkode = "NB",
            meldeform = "ELEKTRONISK",
        )

        val arenaMeldekortOversikt = arenaMeldekortResponse.tilArenaMeldekortOversikt()

        arenaMeldekortOversikt.personId shouldBe arenaMeldekortResponse.personId
        arenaMeldekortOversikt.etternavn shouldBe arenaMeldekortResponse.etternavn
        arenaMeldekortOversikt.fornavn shouldBe arenaMeldekortResponse.fornavn
        arenaMeldekortOversikt.maalformkode shouldBe arenaMeldekortResponse.maalformkode
        arenaMeldekortOversikt.meldeform shouldBe arenaMeldekortResponse.meldeform
        arenaMeldekortOversikt.meldekortListe shouldBe arenaMeldekortResponse.meldekortListe
    }
}
