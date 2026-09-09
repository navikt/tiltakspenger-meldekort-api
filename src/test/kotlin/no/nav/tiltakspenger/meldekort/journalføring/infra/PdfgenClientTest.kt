package no.nav.tiltakspenger.meldekort.journalføring.infra

import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.withWireMockServer
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.meldekort.journalføring.PdfA
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PdfgenClientTest {
    private val pdfContent = "dette er innholdet i pdf vi får tilbake fra pdfGen".toByteArray()

    private fun klient(transport: FakeHttpTransport) = PdfgenClientImpl(
        baseUrl = "http://pdfgenrs",
        clock = fixedClock,
        transport = transport,
    )

    private fun FakeHttpTransport.leggIKøPdf() = leggIKøBytes(pdfContent, contentType = "application/pdf")

    @Nested
    inner class GenererMeldekortPdf {
        @Test
        fun `genererer pdf over ekte transport mot wiremock`() {
            // Uten `transport` bruker klienten `JavaHttpTransport`, så denne testen dekker produksjonsoppsettet ende til ende.
            // Ellers ble den linja bare nådd når journalføringsjobben rakk å kjøre under `ApplicationTest`, og dekningsgaten ble avhengig av timing.
            withWireMockServer { wiremock ->
                wiremock.post {
                    url equalTo "/$PDFGEN_PATH/meldekort"
                } returns {
                    statusCode = 200
                    header = "Content-Type" to "application/pdf"
                    body = String(pdfContent)
                }

                runTest {
                    val klient = PdfgenClientImpl(
                        baseUrl = wiremock.baseUrl(),
                        clock = fixedClock,
                    )

                    val resp = klient.genererMeldekortPdf(ObjectMother.meldekort()).getOrFail()

                    resp.pdf.toBase64() shouldBe PdfA(pdfContent).toBase64()
                }
            }
        }

        @Test
        fun `genererer pdf fra pdfgenrs`() = runTest {
            val transport = FakeHttpTransport()
            transport.leggIKøPdf()

            val resp = klient(transport).genererMeldekortPdf(ObjectMother.meldekort()).getOrFail()

            resp.pdf.toBase64() shouldBe PdfA(pdfContent).toBase64()
            transport.mottatteKall.single().uri.toString() shouldBe "http://pdfgenrs/api/v1/genpdf/tpts/meldekort"
        }

        @Test
        fun `feilstatus gir UventetStatus`() = runTest {
            val transport = FakeHttpTransport()
            transport.leggIKøStatus(404, body = "ikke funnet")

            val feil = klient(transport).genererMeldekortPdf(ObjectMother.meldekort())
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

            val resp = klient(transport)
                .genererKorrigertMeldekortPdf(ObjectMother.meldekort(korrigering = true, locale = "en"))
                .getOrFail()

            resp.pdf.toBase64() shouldBe PdfA(pdfContent).toBase64()
            transport.mottatteKall.single().uri.toString() shouldBe "http://pdfgenrs/api/v1/genpdf/tpts/meldekort-korrigert-en"
        }

        @Test
        fun `feilstatus gir UventetStatus`() = runTest {
            val transport = FakeHttpTransport()
            transport.leggIKøStatus(404, body = "ikke funnet")

            val feil = klient(transport)
                .genererKorrigertMeldekortPdf(ObjectMother.meldekort(korrigering = true))
                .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
                .value

            feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>().statusCode shouldBe 404
        }
    }
}
