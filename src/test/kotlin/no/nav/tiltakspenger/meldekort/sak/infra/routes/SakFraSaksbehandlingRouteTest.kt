package no.nav.tiltakspenger.meldekort.sak.infra.routes

import arrow.core.left
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.http.withCharset
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.TestApplicationContextMedInMemoryDb
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.infra.routes.ktorSetup
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContext
import no.nav.tiltakspenger.meldekort.sak.FeilVedMottakAvSak
import no.nav.tiltakspenger.meldekort.sak.LagreFraSaksbehandlingService
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import org.junit.jupiter.api.Test

class SakFraSaksbehandlingRouteTest {

    private val periode = 6 til 19.januar(2025)

    @Test
    fun `mottaSak - oppretter sak og meldekort`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            mottaSakRequest(
                tac = tac,
                requestDto = ObjectMother.sakDTO(
                    meldeperioder = listOf(ObjectMother.meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
                ),
                forventetStatus = HttpStatusCode.OK,
                forventetBody = "Sak lagret",
                forventetContentType = ContentType.Text.Plain.withCharset(Charsets.UTF_8),
            )
        }
    }

    @Test
    fun `mottaSak - returnerer 400 ved ugyldig JSON-body`() = runTest {
        withTestApplicationContext { _ ->
            defaultRequestWithAssertions(
                method = HttpMethod.Post,
                uri = url {
                    protocol = URLProtocol.HTTPS
                    path("/saksbehandling/sak")
                },
                jwt = JwtGenerator().createJwtForSystembruker(),
                forventetStatus = HttpStatusCode.BadRequest,
                forventetBody = "Feil ved parsing av sak fra saksbehandling-api",
                forventetContentType = ContentType.Text.Plain.withCharset(Charsets.UTF_8),
            ) {
                setBody("{ ikke gyldig json")
            }
        }
    }

    @Test
    fun `mottaSak - returnerer 400 dersom DTO bryter domeneinvariant`() = runTest {
        withTestApplicationContext { _ ->
            // SakId.fromString på ugyldig id kaster IllegalArgumentException → 400 (klientfeil),
            // ikke 500. Slik unngår avsender unødvendige retries og alarmer.
            val ugyldigDto = ObjectMother.sakDTO(sakId = "ikke-en-gyldig-sakid")
            defaultRequestWithAssertions(
                method = HttpMethod.Post,
                uri = url {
                    protocol = URLProtocol.HTTPS
                    path("/saksbehandling/sak")
                },
                jwt = JwtGenerator().createJwtForSystembruker(),
                forventetStatus = HttpStatusCode.BadRequest,
                forventetBody = "Ugyldig sak fra saksbehandling-api (brudd på domeneinvariant). sakId: ikke-en-gyldig-sakid",
                forventetContentType = ContentType.Text.Plain.withCharset(Charsets.UTF_8),
            ) {
                setBody(serialize(ugyldigDto))
            }
        }
    }

    @Test
    fun `mottaSak - returnerer 500 ved LagringFeilet fra service`() = runTest {
        val mockedService = mockk<LagreFraSaksbehandlingService>()
        every { mockedService.lagre(any()) } returns FeilVedMottakAvSak.LagringFeilet.left()

        val context = object : TestApplicationContextMedInMemoryDb() {
            override val lagreFraSaksbehandlingService = mockedService
        }

        testApplication {
            application { ktorSetup(context) }
            defaultRequestWithAssertions(
                method = HttpMethod.Post,
                uri = url {
                    protocol = URLProtocol.HTTPS
                    path("/saksbehandling/sak")
                },
                jwt = JwtGenerator().createJwtForSystembruker(),
                forventetStatus = HttpStatusCode.InternalServerError,
                forventetBody = "Lagring av sak feilet",
                forventetContentType = ContentType.Text.Plain.withCharset(Charsets.UTF_8),
            ) {
                setBody(serialize(ObjectMother.sakDTO()))
            }
        }
    }
}
