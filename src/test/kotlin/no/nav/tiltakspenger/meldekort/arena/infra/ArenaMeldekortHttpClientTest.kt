package no.nav.tiltakspenger.meldekort.arena.infra

import arrow.core.right
import com.github.tomakehurst.wiremock.WireMockServer
import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.get
import com.marcinziolo.kotlin.wiremock.returns
import com.marcinziolo.kotlin.wiremock.returnsJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.withWireMockServer
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.testutils.testTokenProvider
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * WireMock-test for [ArenaMeldekortHttpClient].
 *
 * Verifiserer HTTP-flyten mot arena meldekortservice:
 *  - 200 deserialiseres til [no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortOversikt].
 *  - 204 (No Content) på /meldekort gir `null` (ingen meldekort i arena).
 *  - Andre statuser på /meldekort gir [HttpKlientError.UventetStatus].
 *  - Feilstatus på /historiskemeldekort gir `null` (brukeren finnes ikke).
 *  - Transportfeil gir [HttpKlientError.IngenRespons].
 */
internal class ArenaMeldekortHttpClientTest {
    private val fnr = Fnr.fromString("12345678910")

    private val okBody = """
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

    private fun WireMockServer.client() = ArenaMeldekortHttpClient(
        baseUrl = baseUrl(),
        clock = fixedClock,
        authTokenProvider = testTokenProvider,
    )

    @Test
    fun `hentMeldekort - 200 deserialiseres og sender ident og Bearer-token`() {
        withWireMockServer { wiremock ->
            wiremock.get {
                url equalTo "/meldekortservice/api/v2/meldekort"
                headers contains "Authorization" equalTo "Bearer test-token"
                headers contains "ident" equalTo fnr.verdi
            } returnsJson {
                statusCode = 200
                body = okBody
            }

            runTest {
                val oversikt = wiremock.client().hentMeldekort(fnr).getOrNull()!!
                oversikt.personId shouldBe 123L
                oversikt.meldekortListe!!.single().fraDato shouldBe LocalDate.parse("2025-01-06")
            }
        }
    }

    @Test
    fun `hentMeldekort - 204 gir null`() {
        withWireMockServer { wiremock ->
            wiremock.get {
                url equalTo "/meldekortservice/api/v2/meldekort"
            } returns {
                statusCode = 204
            }

            runTest {
                wiremock.client().hentMeldekort(fnr) shouldBe null.right()
            }
        }
    }

    @Test
    fun `hentMeldekort - annen feilstatus gir UventetStatus`() {
        withWireMockServer { wiremock ->
            wiremock.get {
                url equalTo "/meldekortservice/api/v2/meldekort"
            } returns {
                statusCode = 500
            }

            runTest {
                val feil = wiremock.client().hentMeldekort(fnr)
                    .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
                    .value

                feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 500
            }
        }
    }

    @Test
    fun `hentMeldekort - transportfeil gir IngenRespons`() {
        // Peker mot en port ingen lytter på for å trigge ConnectException.
        val client = ArenaMeldekortHttpClient(
            baseUrl = "http://localhost:1",
            clock = fixedClock,
            authTokenProvider = testTokenProvider,
        )

        runTest {
            client.hentMeldekort(fnr)
                .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
                .value
                .shouldBeInstanceOf<HttpKlientError.IngenRespons>()
        }
    }

    @Test
    fun `hentHistoriskeMeldekort - 200 deserialiseres`() {
        withWireMockServer { wiremock ->
            wiremock.get {
                url equalTo "/meldekortservice/api/v2/historiskemeldekort?antallMeldeperioder=10"
            } returnsJson {
                statusCode = 200
                body = okBody
            }

            runTest {
                wiremock.client().hentHistoriskeMeldekort(fnr).getOrNull()!!.personId shouldBe 123L
            }
        }
    }

    @Test
    fun `hentHistoriskeMeldekort - feilstatus gir null`() {
        withWireMockServer { wiremock ->
            wiremock.get {
                url equalTo "/meldekortservice/api/v2/historiskemeldekort?antallMeldeperioder=10"
            } returns {
                statusCode = 503
            }

            runTest {
                wiremock.client().hentHistoriskeMeldekort(fnr) shouldBe null.right()
            }
        }
    }

    @Test
    fun `hentHistoriskeMeldekort - transportfeil gir IngenRespons`() {
        val client = ArenaMeldekortHttpClient(
            baseUrl = "http://localhost:1",
            clock = fixedClock,
            authTokenProvider = testTokenProvider,
        )

        runTest {
            client.hentHistoriskeMeldekort(fnr)
                .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
                .value
                .shouldBeInstanceOf<HttpKlientError.IngenRespons>()
        }
    }
}
