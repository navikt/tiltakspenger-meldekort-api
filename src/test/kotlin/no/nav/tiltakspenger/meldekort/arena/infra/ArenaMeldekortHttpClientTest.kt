package no.nav.tiltakspenger.meldekort.arena.infra

import arrow.core.left
import arrow.core.right
import com.github.tomakehurst.wiremock.WireMockServer
import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.get
import com.marcinziolo.kotlin.wiremock.returns
import com.marcinziolo.kotlin.wiremock.returnsJson
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortServiceFeil
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

/**
 * WireMock-test for [ArenaMeldekortHttpClient].
 *
 * Verifiserer HTTP-flyten mot arena meldekortservice:
 *  - 200 deserialiseres til [no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortOversikt].
 *  - 204 (No Content) på /meldekort gir `null` (ingen meldekort i arena).
 *  - Andre statuser på /meldekort gir [ArenaMeldekortServiceFeil.HttpFeil].
 *  - Feil på /historiskemeldekort gir `null` (vi bryr oss ikke om feilhåndtering der).
 *  - Exception ved utsending fanges og mappes til [ArenaMeldekortServiceFeil.UkjentFeil].
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

    private fun accessToken(token: String = "test-token") = AccessToken(
        token = token,
        expiresAt = Instant.MAX,
        invaliderCache = {},
    )

    private fun withWireMockServer(block: (WireMockServer) -> Unit) {
        val server = WireMockServer(0)
        server.start()
        try {
            block(server)
        } finally {
            server.stop()
        }
    }

    private fun WireMockServer.client() = ArenaMeldekortHttpClient(
        baseUrl = baseUrl(),
        getToken = { accessToken() },
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
    fun `hentMeldekort - annen feilstatus gir HttpFeil`() {
        withWireMockServer { wiremock ->
            wiremock.get {
                url equalTo "/meldekortservice/api/v2/meldekort"
            } returns {
                statusCode = 500
            }

            runTest {
                wiremock.client().hentMeldekort(fnr) shouldBe ArenaMeldekortServiceFeil.HttpFeil(500).left()
            }
        }
    }

    @Test
    fun `hentMeldekort - exception ved utsending gir UkjentFeil`() {
        // Peker mot en port ingen lytter på for å trigge ConnectException som fanges av Either.catch.
        val client = ArenaMeldekortHttpClient(
            baseUrl = "http://localhost:1",
            getToken = { accessToken() },
        )

        runTest {
            client.hentMeldekort(fnr) shouldBe ArenaMeldekortServiceFeil.UkjentFeil.left()
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
    fun `hentHistoriskeMeldekort - exception ved utsending gir UkjentFeil`() {
        val client = ArenaMeldekortHttpClient(
            baseUrl = "http://localhost:1",
            getToken = { accessToken() },
        )

        runTest {
            client.hentHistoriskeMeldekort(fnr) shouldBe ArenaMeldekortServiceFeil.UkjentFeil.left()
        }
    }
}
