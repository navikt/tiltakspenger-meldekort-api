package no.nav.tiltakspenger.meldekort.api.service.routes

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.service.MeldekortServiceImpl
import no.nav.tiltakspenger.meldekort.api.service.testApplikasjon
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class RoutesTokenValidationTest {
    companion object {
        private val mockOAuth2Server = MockOAuth2Server().also {
            it.start(8080)
        }

        @AfterAll
        @JvmStatic
        fun teardown() = mockOAuth2Server.shutdown()
    }

    private fun token(
        issuerId: String = "azure",
        audience: String = "validAudience",
        expiry: Long = 3600L,
    ): SignedJWT = mockOAuth2Server
        .issueToken(
            issuerId = issuerId,
            audience = audience,
            expiry = expiry,
        )

    private val gyldigAzureToken: SignedJWT = token()

    private val utgåttAzureToken: SignedJWT = token(expiry = -60L)

    private val tokenMedFeilIssuer: SignedJWT = token(issuerId = "feilIssuer")

    private val tokenMedFeilAudience: SignedJWT = token(audience = "feilAudience")

    private val vedtakRequestBody = """
        {
            "ident": "12345678910",
            "fom": "2021-01-01",
            "tom": "2021-01-31"
        }
    """.trimIndent()

    @Test
    fun `get med ugyldig token skal gi 401`() {
        testApplication {
            testApplikasjon()
            val response = client.get("/meldekort/hentMeldekort/123456") {
                header("Authorization", "Bearer tulletoken")
                header("Content-Type", "application/json")
                setBody(vedtakRequestBody)
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `get med utgått token skal gi 401`() {
        testApplication {
            testApplikasjon()
            val response = client.get("/meldekort/hentMeldekort/123456") {
                header("Authorization", "Bearer ${utgåttAzureToken.serialize()}")
                header("Content-Type", "application/json")
                setBody(vedtakRequestBody)
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `get med feil issuer token skal gi 401`() {
        testApplication {
            testApplikasjon()
            val response = client.get("/meldekort/hentMeldekort/123456") {
                header("Authorization", "Bearer ${tokenMedFeilIssuer.serialize()}")
                header("Content-Type", "application/json")
                setBody(vedtakRequestBody)
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `get med feil audience token skal gi 401`() {
        testApplication {
            testApplikasjon()
            val response = client.get("/meldekort/hentMeldekort/123456") {
                header("Authorization", "Bearer ${tokenMedFeilAudience.serialize()}")
                header("Content-Type", "application/json")
                setBody(vedtakRequestBody)
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `get med gyldig token skal gi 200`() {
        val meldekortServiceMock = mockk<MeldekortServiceImpl>(relaxed = true).also {
            coEvery { it.hentMeldekort(any()) } returns Meldekort.Åpent(
                id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
                løpenr = 1,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                forrigeMeldekort = null,
                meldekortDager = emptyList(),
                sistEndret = LocalDateTime.now(),
                opprettet = LocalDateTime.now(),
            )
        }

        testApplication {
            testApplikasjon(meldekortService = meldekortServiceMock)
            val response = client.get("/meldekort/hentMeldekort/00000000-0000-0000-0000-000000000000") {
                header("Authorization", "Bearer ${gyldigAzureToken.serialize()}")
                header("Content-Type", "application/json")
                setBody(vedtakRequestBody)
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }
}
