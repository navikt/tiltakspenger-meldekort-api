package no.nav.tiltakspenger.meldekort.clients.dokarkiv

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.meldekort.clients.httpClientGeneric
import no.nav.tiltakspenger.meldekort.domene.journalføring.PdfA
import no.nav.tiltakspenger.meldekort.domene.journalføring.PdfOgJson
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import toBrevMeldekortDTO
import java.time.Instant

class DokarkivClientTest {
    private val journalpostId = "1"
    private val baseurl = "http://dokarkiv"

    private val svarFerdigstilt = """
        {
          "journalpostId": "$journalpostId",
          "journalpostferdigstilt": true
        }
    """.trimIndent()

    private val svarIkkeFerdigstilt = """
        {
          "journalpostId": "$journalpostId",
          "journalpostferdigstilt": false
        }
    """.trimIndent()

    private fun getMockToken(): AccessToken {
        return AccessToken("token", Instant.now().plusSeconds(3600)) {}
    }

    private fun createDokarkivClient(mockResponse: String, status: HttpStatusCode): DokarkivClient {
        val mockEngine = MockEngine {
            respond(
                content = mockResponse,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = httpClientGeneric(mockEngine)
        return DokarkivClient(
            client = client,
            baseUrl = baseurl,
            getToken = { getMockToken() },
        )
    }

    @Nested
    inner class JournalførMeldekort {
        @Test
        fun `skal ferdigstille, blir opprettet - returnerer journalpostid`() = runTest {
            val dokarkivClient = createDokarkivClient(svarFerdigstilt, HttpStatusCode.Created)
            val meldekort = ObjectMother.meldekort()

            val resp = dokarkivClient.journalførMeldekort(
                request = meldekort.toJournalpostDokument(pdfOgJson = PdfOgJson(PdfA("pdf".toByteArray()), meldekort.toBrevMeldekortDTO())),
                meldekort = meldekort,
                callId = CorrelationId.generate(),
            )

            resp.toString() shouldBe journalpostId
        }

        @Test
        fun `skal ikke ferdigstille uten saksnummer, men blir opprettet - returnerer journalpostid`() = runTest {
            val dokarkivClient = createDokarkivClient(svarIkkeFerdigstilt, HttpStatusCode.Created)
            val meldekort = ObjectMother.meldekort(saksnummer = null)

            val resp = dokarkivClient.journalførMeldekort(
                request = meldekort.toJournalpostDokument(pdfOgJson = PdfOgJson(PdfA("pdf".toByteArray()), meldekort.toBrevMeldekortDTO())),
                meldekort = meldekort,
                callId = CorrelationId.generate(),
            )

            resp.toString() shouldBe journalpostId
        }

        @Test
        fun `ved 409 Conflict returneres journalPostId`() = runTest {
            val dokarkivClient = createDokarkivClient(svarIkkeFerdigstilt, HttpStatusCode.Conflict)
            val meldekort = ObjectMother.meldekort()

            val resp = dokarkivClient.journalførMeldekort(
                request = meldekort.toJournalpostDokument(pdfOgJson = PdfOgJson(PdfA("pdf".toByteArray()), meldekort.toBrevMeldekortDTO())),
                meldekort = meldekort,
                callId = CorrelationId.generate(),
            )

            resp.toString() shouldBe journalpostId
        }
    }
}
