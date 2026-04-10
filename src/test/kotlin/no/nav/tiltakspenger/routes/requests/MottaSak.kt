package no.nav.tiltakspenger.routes.requests

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.routes.JwtGenerator
import no.nav.tiltakspenger.routes.defaultRequest

/**
 * Route: [no.nav.tiltakspenger.meldekort.routes.meldekort.saksbehandling.sakFraSaksbehandlingRoute]
 * Dto: [no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO]
 */
suspend fun ApplicationTestBuilder.mottaSakRequest(
    fnr: Fnr,
    sakId: SakId,
    saksnummer: String,
    meldeperioder: List<SakTilMeldekortApiDTO.Meldeperiode> = listOf(meldeperiodeDto()),
    harSoknadUnderBehandling: Boolean = false,
    kanSendeInnHelgForMeldekort: Boolean = false,
    forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
) {
    return mottaSakRequest(
        dto = SakTilMeldekortApiDTO(
            fnr = fnr.verdi,
            sakId = sakId.toString(),
            saksnummer = saksnummer,
            meldeperioder = meldeperioder,
            harSoknadUnderBehandling = harSoknadUnderBehandling,
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
        ),
        forventetStatus = forventetStatus,
    )
}

/**
 * Route: [no.nav.tiltakspenger.meldekort.routes.meldekort.saksbehandling.sakFraSaksbehandlingRoute]
 * Dto: [no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO]
 */
suspend fun ApplicationTestBuilder.mottaSakRequest(
    dto: SakTilMeldekortApiDTO,
    forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
    forventetBody: String? = "Sak lagret",
) {
    defaultRequest(
        method = HttpMethod.Post,
        uri = url {
            protocol = URLProtocol.HTTPS
            path("/saksbehandling/sak")
        },
        jwt = JwtGenerator().createJwtForSystembruker(),
    ) {
        setBody(serialize(dto))
    }.run {
        val bodyAsText = this.bodyAsText()
        withClue(
            "Response details:\n" +
                "Status: ${this.status}\n" +
                "Content-Type: ${this.contentType()}\n" +
                "Body: $bodyAsText",
        ) {
            status shouldBe forventetStatus
            if (forventetBody != null) {
                bodyAsText shouldBe forventetBody
            }
        }
    }
}
