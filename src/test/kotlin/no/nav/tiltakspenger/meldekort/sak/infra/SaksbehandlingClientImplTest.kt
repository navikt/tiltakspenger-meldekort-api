package no.nav.tiltakspenger.meldekort.sak.infra

import arrow.core.right
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.meldekort.sak.SaksbehandlingClient
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.testutils.testTokenProvider
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * Tester [SaksbehandlingClientImpl] over [FakeHttpTransport]: hele klient-pipelinen kjører, kun nettverket byttes ut.
 *
 * Verifiserer HTTP-flyten:
 *  - Suksessresponser (200) returnerer [Unit] som [arrow.core.Either.Right].
 *  - Non-200-responser returnerer [HttpKlientError.UventetStatus] som [arrow.core.Either.Left].
 *  - Transportfeil (f.eks. ingen tilkobling) gir [HttpKlientError.IngenRespons] som [arrow.core.Either.Left].
 */
class SaksbehandlingClientImplTest {

    private val baseUrl = "http://saksbehandling-api"

    private fun klient(transport: FakeHttpTransport) = SaksbehandlingClientImpl(
        baseUrl = baseUrl,
        clock = fixedClock,
        authTokenProvider = testTokenProvider,
        transport = transport,
    )

    @Test
    fun `200-respons gir Right(Unit) og sender Bearer-token og Content-Type JSON`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøTomRespons(statusCode = 200) }

        klient(transport).sendMeldekort(ObjectMother.meldekort()) shouldBe Unit.right()

        val kall = transport.mottatteKall.single()
        kall.metode shouldBe "POST"
        kall.uri.toString() shouldBe "$baseUrl/meldekort/motta"
        kall.request.headers().firstValue("Authorization").get() shouldBe "Bearer test-token"
        kall.request.headers().firstValue("Content-Type").get() shouldBe "application/json"
    }

    @Test
    fun `non-200-respons gir UventetStatus`() = runTest {
        val transport = FakeHttpTransport().apply {
            leggIKøStatus(statusCode = 500, body = "Internal Server Error", contentType = "text/plain")
        }

        val feil = klient(transport).sendMeldekort(ObjectMother.meldekort())
            .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
            .value

        feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 500
    }

    @Test
    fun `transportfeil gir IngenRespons`() = runTest {
        val transport = FakeHttpTransport().apply { leggIKøKast(IOException("connection refused")) }

        val feil = klient(transport).sendMeldekort(ObjectMother.meldekort())
            .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
            .value

        feil.shouldBeInstanceOf<HttpKlientError.IngenRespons>()
    }

    /**
     * Dekker default-verdien for `transport`, altså produksjonsoppkoblingen.
     * De øvrige testene sender inn [FakeHttpTransport], så uten denne ville linja stått udekket.
     * Å bygge klienten rører ingenting på nettverket.
     */
    @Test
    fun `kan bygges med produksjonstransporten som default`() {
        SaksbehandlingClientImpl(
            baseUrl = baseUrl,
            clock = fixedClock,
            authTokenProvider = testTokenProvider,
        ).shouldBeInstanceOf<SaksbehandlingClient>()
    }
}
