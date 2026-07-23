package no.nav.tiltakspenger.meldekort.journalføring.infra

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.meldekort.journalføring.PdfA
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PdfgenClientTest {
    private val pdfContent = "dette er innholdet i pdf vi får tilbake fra pdfGen".toByteArray()

    private fun klient(transport: FakeHttpTransport, isLocalOrDev: Boolean = true) = PdfgenClientImpl(
        baseUrl = "http://pdfgen",
        pdfgenrsBaseUrl = "http://pdfgenrs",
        isLocalOrDev = isLocalOrDev,
        clock = fixedClock,
        transport = transport,
    )

    private fun FakeHttpTransport.leggIKøPdf() = leggIKøBytes(pdfContent, contentType = "application/pdf")

    @Nested
    inner class GenererMeldekortPdf {
        @Test
        fun `lokalt eller i dev genereres pdf fra begge pdfgen-appene`() = runTest {
            val transport = FakeHttpTransport()
            transport.leggIKøPdf()
            transport.leggIKøPdf()

            val resp = klient(transport).genererMeldekortPdf(ObjectMother.meldekort()).getOrFail()

            resp.first.pdf.toBase64() shouldBe PdfA(pdfContent).toBase64()
            resp.second!!.pdf.toBase64() shouldBe PdfA(pdfContent).toBase64()
            transport.mottatteKall.map { it.uri.toString() }.toSet() shouldBe setOf(
                "http://pdfgen/api/v1/genpdf/tpts/meldekort",
                "http://pdfgenrs/api/v1/genpdf/tpts/meldekort",
            )
        }

        @Test
        fun `i prod genereres pdf kun fra pdfgen`() = runTest {
            val transport = FakeHttpTransport()
            transport.leggIKøPdf()

            val resp = klient(transport, isLocalOrDev = false).genererMeldekortPdf(ObjectMother.meldekort()).getOrFail()

            resp.first.pdf.toBase64() shouldBe PdfA(pdfContent).toBase64()
            resp.second shouldBe null
            transport.mottatteKall.single().uri.toString() shouldBe "http://pdfgen/api/v1/genpdf/tpts/meldekort"
        }

        @Test
        fun `feilstatus gir UventetStatus`() = runTest {
            val transport = FakeHttpTransport()
            transport.leggIKøStatus(404, body = "ikke funnet")

            val feil = klient(transport, isLocalOrDev = false).genererMeldekortPdf(ObjectMother.meldekort())
                .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
                .value

            feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 404
        }
    }

    @Nested
    inner class GenererKorrigertMeldekortPdf {
        @Test
        fun `bruker korrigert-malen og engelsk variant for engelsk locale`() = runTest {
            val transport = FakeHttpTransport()
            transport.leggIKøPdf()

            val resp = klient(transport, isLocalOrDev = false)
                .genererKorrigertMeldekortPdf(ObjectMother.meldekort(korrigering = true, locale = "en"))
                .getOrFail()

            resp.first.pdf.toBase64() shouldBe PdfA(pdfContent).toBase64()
            transport.mottatteKall.single().uri.toString() shouldBe "http://pdfgen/api/v1/genpdf/tpts/meldekort-korrigert-en"
        }

        @Test
        fun `feilstatus gir UventetStatus`() = runTest {
            val transport = FakeHttpTransport()
            transport.leggIKøStatus(404, body = "ikke funnet")

            val feil = klient(transport, isLocalOrDev = false)
                .genererKorrigertMeldekortPdf(ObjectMother.meldekort(korrigering = true))
                .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
                .value

            feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 404
        }
    }
}
