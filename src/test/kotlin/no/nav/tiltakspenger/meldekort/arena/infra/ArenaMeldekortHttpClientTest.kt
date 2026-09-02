package no.nav.tiltakspenger.meldekort.arena.infra

import arrow.core.right
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.FnrGenerator
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortClient
import no.nav.tiltakspenger.testutils.testTokenProvider
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.LocalDate

/**
 * Tester [ArenaMeldekortHttpClient] over [FakeHttpTransport]: hele klient-pipelinen kjører, kun nettverket byttes ut.
 *
 * Verifiserer HTTP-flyten mot arena meldekortservice:
 *  - 200 deserialiseres til [no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortOversikt].
 *  - 204 (No Content) på /meldekort gir `null` (ingen meldekort i arena).
 *  - Andre statuser på /meldekort gir [HttpKlientError.UventetStatus].
 *  - Feilstatus på /historiskemeldekort gir `null` (brukeren finnes ikke).
 *  - Transportfeil gir [HttpKlientError.IngenRespons].
 */
class ArenaMeldekortHttpClientTest {
    private val fnrGenerator = FnrGenerator()
    private val fnr = fnrGenerator.generer()

    private val baseUrl = "http://meldekortservice"

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

    private fun client(transport: FakeHttpTransport) = ArenaMeldekortHttpClient(
        baseUrl = baseUrl,
        clock = fixedClock,
        authTokenProvider = testTokenProvider,
        transport = transport,
    )

    @Test
    fun `hentMeldekort - 200 deserialiseres og sender ident og Bearer-token`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson(okBody) }

        val oversikt = client(transport).hentMeldekort(fnr).getOrNull()!!

        oversikt.personId shouldBe 123L
        oversikt.meldekortListe!!.single().fraDato shouldBe LocalDate.parse("2025-01-06")

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "GET"
        kall.uri.toString() shouldBe "$baseUrl/meldekortservice/api/v2/meldekort"
        kall.request.headers().firstValue("Authorization").get() shouldBe "Bearer test-token"
        kall.request.headers().firstValue("ident").get() shouldBe fnr.verdi
    }

    @Test
    fun `hentMeldekort - 204 gir null`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøTomRespons(statusCode = 204) }

        client(transport).hentMeldekort(fnr) shouldBe null.right()
    }

    @Test
    fun `hentMeldekort - annen feilstatus gir UventetStatus`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøStatusForAlleForsøk(statusCode = 500) }

        val feil = client(transport).hentMeldekort(fnr)
            .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
            .value

        feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 500
    }

    @Test
    fun `hentMeldekort - transportfeil gir IngenRespons`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøKastForAlleForsøk(IOException("connection refused")) }

        client(transport).hentMeldekort(fnr)
            .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
            .value
            .shouldBeInstanceOf<HttpKlientError.IngenRespons>()
    }

    @Test
    fun `hentHistoriskeMeldekort - 200 deserialiseres`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøJson(okBody) }

        client(transport).hentHistoriskeMeldekort(fnr).getOrNull()!!.personId shouldBe 123L

        transport.mottatteKall.single().uri.toString() shouldBe
            "$baseUrl/meldekortservice/api/v2/historiskemeldekort?antallMeldeperioder=10"
    }

    @Test
    fun `hentHistoriskeMeldekort - feilstatus gir null`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøStatusForAlleForsøk(statusCode = 503) }

        client(transport).hentHistoriskeMeldekort(fnr) shouldBe null.right()
    }

    @Test
    fun `hentHistoriskeMeldekort - transportfeil gir IngenRespons`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøKastForAlleForsøk(IOException("connection refused")) }

        client(transport).hentHistoriskeMeldekort(fnr)
            .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
            .value
            .shouldBeInstanceOf<HttpKlientError.IngenRespons>()
    }

    /**
     * Dekker default-verdien for `transport`, altså produksjonsoppkoblingen.
     * De øvrige testene sender inn [FakeHttpTransport], så uten denne ville linja stått udekket.
     * Å bygge klienten rører ingenting på nettverket.
     */
    @Test
    fun `kan bygges med produksjonstransporten som default`() {
        ArenaMeldekortHttpClient(
            baseUrl = baseUrl,
            clock = fixedClock,
            authTokenProvider = testTokenProvider,
        ).shouldBeInstanceOf<ArenaMeldekortClient>()
    }
}
