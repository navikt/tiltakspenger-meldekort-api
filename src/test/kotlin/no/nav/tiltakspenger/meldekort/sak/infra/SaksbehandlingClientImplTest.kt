package no.nav.tiltakspenger.meldekort.sak.infra

import arrow.core.right
import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.withWireMockServer
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.testutils.testTokenProvider
import org.junit.jupiter.api.Test

/**
 * WireMock-test for [SaksbehandlingClientImpl] med default `HttpKlient`-oppsett.
 *
 * Verifiserer HTTP-flyten:
 *  - Suksessresponser (200) returnerer [Unit] som [arrow.core.Either.Right].
 *  - Non-200-responser returnerer [HttpKlientError.UventetStatus] som [arrow.core.Either.Left].
 *  - Transportfeil (f.eks. ingen tilkobling) gir [HttpKlientError.IngenRespons] som [arrow.core.Either.Left].
 */
internal class SaksbehandlingClientImplTest {

    private fun klient(baseUrl: String) = SaksbehandlingClientImpl(
        baseUrl = baseUrl,
        clock = fixedClock,
        authTokenProvider = testTokenProvider,
    )

    @Test
    fun `200-respons gir Right(Unit) og sender Bearer-token og Content-Type JSON`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/meldekort/motta"
                headers contains "Authorization" equalTo "Bearer test-token"
                headers contains "Content-Type" equalTo "application/json"
            } returns {
                statusCode = 200
            }

            val meldekort = ObjectMother.meldekort()

            runTest {
                klient(wiremock.baseUrl()).sendMeldekort(meldekort) shouldBe Unit.right()
            }
        }
    }

    @Test
    fun `non-200-respons gir UventetStatus`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/meldekort/motta"
            } returns {
                statusCode = 500
                body = "Internal Server Error"
            }

            val meldekort = ObjectMother.meldekort()

            runTest {
                val feil = klient(wiremock.baseUrl()).sendMeldekort(meldekort)
                    .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
                    .value

                feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 500
            }
        }
    }

    @Test
    fun `transportfeil gir IngenRespons`() {
        // Peker mot en port ingen lytter på for å trigge ConnectException.
        val client = klient("http://localhost:1")

        val meldekort = ObjectMother.meldekort()

        runTest {
            val feil = client.sendMeldekort(meldekort)
                .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
                .value

            feil.shouldBeInstanceOf<HttpKlientError.IngenRespons>()
        }
    }
}
