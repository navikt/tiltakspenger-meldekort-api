package no.nav.tiltakspenger.meldekort.clients.pdfgen

import arrow.core.left
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.meldekort.clients.httpClientGeneric
import no.nav.tiltakspenger.meldekort.domene.journalføring.KunneIkkeGenererePdf
import no.nav.tiltakspenger.meldekort.domene.journalføring.PdfA
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PdfgenClientTest {
    private val pdfContent = "dette er innholdet i pdf vi får tilbake fra pdfGen".toByteArray()

    private fun createMockEngine(content: ByteArray, status: HttpStatusCode, contentType: ContentType): MockEngine {
        return MockEngine {
            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, contentType.toString()),
            )
        }
    }

    private fun createPdfgenClient(mockEngine: MockEngine): PdfgenClient {
        val client = httpClientGeneric(mockEngine)
        return PdfgenClient(client = client)
    }

    @Nested
    inner class GenererMeldekortPdf {
        @Test
        fun `får tilbake en pdf hvis alt går ok`() = runTest {
            val mockEngine = createMockEngine(pdfContent, HttpStatusCode.OK, ContentType.Application.Pdf)
            val pdfgenClient = createPdfgenClient(mockEngine)

            val resp = pdfgenClient.genererMeldekortPdf(ObjectMother.meldekort())
            val pdf = resp.getOrNull()?.pdf

            pdf?.toBase64() shouldBe PdfA(pdfContent).toBase64()
            resp.getOrNull()!!
        }

        @Test
        fun `kaster en feil hvis generering av pdf ikke går ok`() = runTest {
            val mockEngine = createMockEngine(ByteArray(0), HttpStatusCode.NotFound, ContentType.Application.Json)
            val pdfgenClient = createPdfgenClient(mockEngine)

            pdfgenClient.genererMeldekortPdf(ObjectMother.meldekort()) shouldBe KunneIkkeGenererePdf.left()
        }
    }

    @Nested
    inner class GenererKorrigertMeldekortPdf {
        @Test
        fun `får tilbake en pdf hvis alt går ok`() = runTest {
            val mockEngine = createMockEngine(pdfContent, HttpStatusCode.OK, ContentType.Application.Pdf)
            val pdfgenClient = createPdfgenClient(mockEngine)

            val resp = pdfgenClient.genererKorrigertMeldekortPdf(ObjectMother.meldekort(korrigering = true))
            val pdf = resp.getOrNull()?.pdf

            pdf?.toBase64() shouldBe PdfA(pdfContent).toBase64()
            resp.getOrNull()!!
        }

        @Test
        fun `kaster en feil hvis generering av pdf ikke går ok`() = runTest {
            val mockEngine = createMockEngine(ByteArray(0), HttpStatusCode.NotFound, ContentType.Application.Json)
            val pdfgenClient = createPdfgenClient(mockEngine)

            pdfgenClient.genererKorrigertMeldekortPdf(ObjectMother.meldekort(korrigering = true)) shouldBe KunneIkkeGenererePdf.left()
        }
    }
}
