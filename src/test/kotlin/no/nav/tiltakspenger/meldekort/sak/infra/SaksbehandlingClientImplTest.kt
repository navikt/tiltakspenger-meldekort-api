package no.nav.tiltakspenger.meldekort.sak.infra

import arrow.core.left
import arrow.core.right
import com.github.tomakehurst.wiremock.WireMockServer
import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.meldekort.sak.SaksbehandlingApiError
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * WireMock-test for [SaksbehandlingClientImpl].
 *
 * Verifiserer HTTP-flyten:
 *  - Suksessresponser (200) returnerer [Unit] som [arrow.core.Either.Right].
 *  - Non-200-responser returnerer [SaksbehandlingApiError] som [arrow.core.Either.Left].
 *  - Exception ved utsending (f.eks. ingen tilkobling) fanges og mappes til [arrow.core.Either.Left].
 */
internal class SaksbehandlingClientImplTest {

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

            val client = SaksbehandlingClientImpl(
                baseUrl = wiremock.baseUrl(),
                getToken = { accessToken() },
            )

            val meldekort = ObjectMother.meldekort()

            runTest {
                client.sendMeldekort(meldekort) shouldBe Unit.right()
            }
        }
    }

    @Test
    fun `non-200-respons gir SaksbehandlingApiError`() {
        withWireMockServer { wiremock ->
            wiremock.post {
                url equalTo "/meldekort/motta"
            } returns {
                statusCode = 500
                body = "Internal Server Error"
            }

            val client = SaksbehandlingClientImpl(
                baseUrl = wiremock.baseUrl(),
                getToken = { accessToken() },
            )

            val meldekort = ObjectMother.meldekort()

            runTest {
                client.sendMeldekort(meldekort) shouldBe SaksbehandlingApiError.left()
            }
        }
    }

    @Test
    fun `exception ved utsending gir SaksbehandlingApiError`() {
        // Peker mot en port ingen lytter på for å trigge ConnectException som fanges av Either.catch
        val client = SaksbehandlingClientImpl(
            baseUrl = "http://localhost:1",
            getToken = { accessToken() },
        )

        val meldekort = ObjectMother.meldekort()

        runTest {
            client.sendMeldekort(meldekort) shouldBe SaksbehandlingApiError.left()
        }
    }
}
