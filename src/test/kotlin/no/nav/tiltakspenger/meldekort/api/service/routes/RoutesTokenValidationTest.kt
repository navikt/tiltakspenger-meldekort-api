package no.nav.tiltakspenger.meldekort.api.service.routes

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
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
import java.util.UUID

internal class RoutesTokenValidationTest {
    companion object {
        private fun findFreePort(): Int {
            java.net.ServerSocket(0).use { socket ->
                return socket.localPort
            }
        }
        private val port = findFreePort()
        private val mockOAuth2Server = MockOAuth2Server().also {
            it.start(port)
        }

        @AfterAll
        @JvmStatic
        fun teardown() = mockOAuth2Server.shutdown()
    }

    private val validOboClaims = mapOf(
        "NAVident" to "12345678910",
        "groups" to listOf(
            "1b3a2c4d-d620-4fcf-a29b-a6cdadf29680",
            "79985315-b2de-40b8-a740-9510796993c6",
        ),
    )

    private fun token(
        issuerId: String = "azure",
        audience: String = "validAudience",
        expiry: Long = 3600L,
        claims: Map<String, Any> = validOboClaims,
    ): SignedJWT = mockOAuth2Server
        .issueToken(
            issuerId = issuerId,
            audience = audience,
            expiry = expiry,
            claims = claims,
        )

    private val gyldigAzureOboToken: SignedJWT = token()

    private val gyldigAzureSystemToken: SignedJWT = token(claims = mapOf("idtyp" to "app"))

    private val utgåttAzureToken: SignedJWT = token(expiry = -60L)

    private val azureTokenMedFeilIssuer: SignedJWT = token(issuerId = "feilIssuer")

    private val azureTokenMedFeilAudience: SignedJWT = token(audience = "feilAudience")

    private val azureTokenUtenNavidentOgIdtyp: SignedJWT = token(claims = emptyMap())

    private val mocketMeldekort: Meldekort = Meldekort.Åpent(
        id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
        løpenr = 1,
        fom = LocalDate.now(),
        tom = LocalDate.now(),
        forrigeMeldekort = null,
        meldekortDager = emptyList(),
        sistEndret = LocalDateTime.now(),
        opprettet = LocalDateTime.now(),
    )

    @Test
    fun `get med ugyldig token skal gi 401`() {
        testApplication {
            testApplikasjon(port = port)
            val response = client.get("/meldekort/hentMeldekort/123456") {
                header("Authorization", "Bearer tulletoken")
                header("Content-Type", "application/json")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `get med utgått token skal gi 401`() {
        testApplication {
            testApplikasjon(port = port)
            val response = client.get("/meldekort/hentMeldekort/123456") {
                header("Authorization", "Bearer ${utgåttAzureToken.serialize()}")
                header("Content-Type", "application/json")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `get med feil issuer token skal gi 401`() {
        testApplication {
            testApplikasjon(port = port)
            val response = client.get("/meldekort/hentMeldekort/123456") {
                header("Authorization", "Bearer ${azureTokenMedFeilIssuer.serialize()}")
                header("Content-Type", "application/json")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `get med feil audience token skal gi 401`() {
        testApplication {
            testApplikasjon(port = port)
            val response = client.get("/meldekort/hentMeldekort/123456") {
                header("Authorization", "Bearer ${azureTokenMedFeilAudience.serialize()}")
                header("Content-Type", "application/json")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `get med token uten NAVident eller idtyp skal gi 401`() {
        testApplication {
            testApplikasjon(port = port)
            val response = client.get("/meldekort/hentMeldekort/123456") {
                header("Authorization", "Bearer ${azureTokenUtenNavidentOgIdtyp.serialize()}")
                header("Content-Type", "application/json")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `get med gyldig OBO-token skal gi 200`() {
        val meldekortServiceMock = mockk<MeldekortServiceImpl>(relaxed = true).also {
            coEvery { it.hentMeldekort(any()) } returns mocketMeldekort
        }

        testApplication {
            testApplikasjon(meldekortService = meldekortServiceMock, port = port)
            val response = client.get("/meldekort/hentMeldekort/00000000-0000-0000-0000-000000000000") {
                header("Authorization", "Bearer ${gyldigAzureOboToken.serialize()}")
                header("Content-Type", "application/json")
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `get med gyldig systemtoken ved innsending av grunnlag skal gi 200`() {
        val meldekortServiceMock = mockk<MeldekortServiceImpl>(relaxed = true)

        testApplication {
            testApplikasjon(meldekortService = meldekortServiceMock, port = port)
            val response = client.post("/meldekort/grunnlag") {
                header("Authorization", "Bearer ${gyldigAzureSystemToken.serialize()}")
                header("Content-Type", "application/json")
                setBody(grunnlagJson)
            }
            assertEquals(HttpStatusCode.OK, response.status, "response body: ${response.bodyAsText()}")
        }
    }

    private val grunnlagJson = """
    {
        "sakId": "123456",
        "vedtakId": "123456",
        "behandlingId": "123",
        "status": "AKTIV",
        "vurderingsperiode": {
            "fra": "2021-01-01",
            "til": "2021-01-14"
        },
        "tiltak": [
            {
                "periodeDTO": {
                    "fra": "2021-01-01",
                    "til": "2021-01-14"
                },
                "typeKode": "GRUPPE_AMO",
                "antDagerIUken": 1.0
            }
        ],
        "personopplysninger": {
            "fornavn": "fornavn",
            "etternavn": "etternavn",
            "ident": "12345678910"
        },
        "utfallsperioder": [
            {
                "fom": "2021-01-01",
                "tom": "2021-01-14",
                "utfall": "GIR_RETT_TILTAKSPENGER"
            }
        ]
    }
    """.trimIndent()
}
