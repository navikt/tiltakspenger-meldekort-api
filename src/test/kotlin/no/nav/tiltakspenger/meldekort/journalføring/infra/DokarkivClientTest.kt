package no.nav.tiltakspenger.meldekort.journalføring.infra

import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.withWireMockServer
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.meldekort.journalføring.PdfA
import no.nav.tiltakspenger.meldekort.journalføring.PdfOgJson
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.testutils.testTokenProvider
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DokarkivClientTest {
    private val journalpostId = "1"

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

    private fun klient(transport: FakeHttpTransport) = DokarkivClientImpl(
        baseUrl = "http://dokarkiv",
        clock = fixedClock,
        authTokenProvider = testTokenProvider,
        transport = transport,
    )

    private suspend fun DokarkivClientImpl.journalfør(meldekort: BrukersMeldekort = ObjectMother.meldekort()) =
        journalførMeldekort(
            meldekort = meldekort,
            pdfOgJson = PdfOgJson(PdfA("pdf".toByteArray()), meldekort.toDTO()),
            callId = CorrelationId.generate(),
            clock = fixedClock,
        )

    @Nested
    inner class JournalførMeldekort {
        @Test
        fun `journalfører med default HttpKlient-oppsett`() {
            withWireMockServer { wiremock ->
                wiremock.post {
                    url equalTo "/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true"
                } returns {
                    statusCode = 201
                    header = "Content-Type" to "application/json"
                    body = svarFerdigstilt
                }

                runTest {
                    val klient = DokarkivClientImpl(
                        baseUrl = wiremock.baseUrl(),
                        clock = fixedClock,
                        authTokenProvider = testTokenProvider,
                    )

                    klient.journalfør().getOrFail().toString() shouldBe journalpostId
                }
            }
        }

        @Test
        fun `201 med ferdigstilt journalpost returnerer journalpostId`() = runTest {
            val transport = FakeHttpTransport()
            transport.leggIKøJson(svarFerdigstilt, statusCode = 201)

            val resp = klient(transport).journalfør().getOrFail()

            resp.toString() shouldBe journalpostId
            transport.mottatteKall.single().uri.toString() shouldContain "forsoekFerdigstill="
        }

        @Test
        fun `201 uten ferdigstilt journalpost returnerer journalpostId og logger driftssignal`() = runTest {
            val transport = FakeHttpTransport()
            transport.leggIKøJson(svarIkkeFerdigstilt, statusCode = 201)

            klient(transport).journalfør().getOrFail().toString() shouldBe journalpostId
        }

        @Test
        fun `ved 409 Conflict returneres journalpostId fra dedup-svaret`() = runTest {
            val transport = FakeHttpTransport()
            transport.leggIKøJson(svarIkkeFerdigstilt, statusCode = 409)

            klient(transport).journalfør().getOrFail().toString() shouldBe journalpostId
        }

        @Test
        fun `feilstatus gir UventetStatus med responsbody`() = runTest {
            val transport = FakeHttpTransport()
            transport.leggIKøStatus(400, body = """{"melding": "ugyldig journalpost"}""")

            val feil = klient(transport).journalfør()
                .shouldBeInstanceOf<arrow.core.Either.Left<HttpKlientError>>()
                .value

            val uventetStatus = feil.shouldBeInstanceOf<HttpKlientError.UventetStatus>()
            uventetStatus.statusCode shouldBe 400
            uventetStatus.body shouldContain "ugyldig journalpost"
        }
    }
}
