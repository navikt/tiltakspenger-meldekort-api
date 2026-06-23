package no.nav.tiltakspenger.dev

import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.NumberFormat
import io.kotest.assertions.json.PropertyOrder
import io.kotest.assertions.json.TypeCoercion
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.TestApplicationContextMedInMemoryDb
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.meldekort.infra.routes.ktorSetup
import org.junit.jupiter.api.Test

/**
 * Tester for det dev-only endepunktet [devRoutes] (`POST /dev/sak`).
 *
 * Endepunktet er bevisst dev-only og kobles kun inn fra LokalMain, men vi tester det likevel for å
 * låse den dev-spesifikke logikken: defaulten med 2 meldeperioder bakover + 2 fremover, og at en sak
 * faktisk blir opprettet via den ekte mottak-flyten (samme service som `POST /saksbehandling/sak`).
 *
 * Vi asserter rett på JSON-strengen (ingen deserialisering) slik at et hvilket som helst kontraktsbrudd mot
 * konsumenten – endret feltnavn, rekkefølge, type e.l. – får testen til å feile. Klokka er fast (1. mai 2025),
 * så meldeperiodene blir deterministiske og kan skrives ut eksplisitt.
 */
class DevRoutesTest {

    // Torsdag 1. mai 2025. Inneværende meldeperiode-mandag er 28. april 2025.
    private val klokke = TikkendeKlokke(fixedClockAt(1.mai(2025)))

    @Test
    fun `oppretter sak med 2 meldeperioder bakover og 2 fremover som default`() = runTest {
        withDevApp { tac ->
            val fnr = "12345678910"
            val sakId = SakId.random().toString()
            val saksnummer = "SAK-1"

            val response = client.post("/dev/sak") {
                setBody("""{ "fnr": "$fnr", "sakId": "$sakId", "saksnummer": "$saksnummer" }""")
            }

            response.status shouldBe HttpStatusCode.Created
            // 2 perioder helt i fortiden (tilOgMed < 1. mai) + 2 fra og med inneværende periode og fremover.
            response.bodyAsText().shouldBeOpprettSakJson(
                """
                {
                  "sakId": "$sakId",
                  "saksnummer": "$saksnummer",
                  "fnr": "$fnr",
                  "meldeperioder": [
                    { "kjedeId": "2025-03-31/2025-04-13", "fraOgMed": "2025-03-31", "tilOgMed": "2025-04-13" },
                    { "kjedeId": "2025-04-14/2025-04-27", "fraOgMed": "2025-04-14", "tilOgMed": "2025-04-27" },
                    { "kjedeId": "2025-04-28/2025-05-11", "fraOgMed": "2025-04-28", "tilOgMed": "2025-05-11" },
                    { "kjedeId": "2025-05-12/2025-05-25", "fraOgMed": "2025-05-12", "tilOgMed": "2025-05-25" }
                  ]
                }
                """.trimIndent(),
            )

            // Saken er faktisk lagret via den ekte mottak-flyten.
            val sak = tac.sakRepo.hent(SakId.fromString(sakId))!!
            sak.meldeperioder.size shouldBe 4
            sak.fnr.verdi shouldBe fnr
        }
    }

    @Test
    fun `respekterer overrides for fnr og antall perioder`() = runTest {
        withDevApp { tac ->
            val fnr = "12345678910"
            val sakId = SakId.random().toString()
            val saksnummer = "SAK-1"

            val response = client.post("/dev/sak") {
                setBody(
                    """
                    {
                      "fnr": "$fnr",
                      "sakId": "$sakId",
                      "saksnummer": "$saksnummer",
                      "antallMeldeperioderBakover": 1,
                      "antallMeldeperioderFremover": 0,
                      "harSoknadUnderBehandling": true
                    }
                    """.trimIndent(),
                )
            }

            response.status shouldBe HttpStatusCode.Created
            response.bodyAsText().shouldBeOpprettSakJson(
                """
                {
                  "sakId": "$sakId",
                  "saksnummer": "$saksnummer",
                  "fnr": "$fnr",
                  "meldeperioder": [
                    { "kjedeId": "2025-04-14/2025-04-27", "fraOgMed": "2025-04-14", "tilOgMed": "2025-04-27" }
                  ]
                }
                """.trimIndent(),
            )

            val sak = tac.sakRepo.hent(SakId.fromString(sakId))!!
            sak.harSoknadUnderBehandling shouldBe true
            sak.meldeperioder.size shouldBe 1
        }
    }

    private fun withDevApp(block: suspend io.ktor.server.testing.ApplicationTestBuilder.(TestApplicationContextMedInMemoryDb) -> Unit) {
        testApplication {
            val tac = TestApplicationContextMedInMemoryDb(clock = klokke)
            application {
                ktorSetup(tac)
                routing { devRoutes(tac) }
            }
            block(tac)
        }
    }
}

/**
 * Asserter mot forventet JSON med streng semantikk (feltnavn, rekkefølge, type), uten å serialisere/deserialisere
 * en DTO. Whitespace ignoreres av JSON-parseren. Samme tilnærming som `String.shouldBeAlleMeldekortJson`.
 */
private fun String.shouldBeOpprettSakJson(forventet: String) {
    this.shouldEqualJson {
        propertyOrder = PropertyOrder.Strict
        arrayOrder = ArrayOrder.Strict
        fieldComparison = FieldComparison.Strict
        numberFormat = NumberFormat.Strict
        typeCoercion = TypeCoercion.Disabled

        forventet
    }
}
