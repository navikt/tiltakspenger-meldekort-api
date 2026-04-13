package no.nav.tiltakspenger.routes.saksbehandling

import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.http.withCharset
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.meldekort.domene.Sak
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.routes.JwtGenerator
import no.nav.tiltakspenger.routes.defaultRequestWithAssertions

/**
 * Route: [no.nav.tiltakspenger.meldekort.routes.meldekort.saksbehandling.sakFraSaksbehandlingRoute]
 * Request DTO: [no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO]
 */
suspend fun ApplicationTestBuilder.mottaSakRequest(
    tac: TestApplicationContext,
    fnr: Fnr = Fnr.fromString(FAKE_FNR),
    sakId: SakId = SakId.random(),
    saksnummer: String = tac.nesteSaksnummer(),
    meldeperioder: List<SakTilMeldekortApiDTO.Meldeperiode> = listOf(meldeperiodeDto(opprettet = nå(tac.clock))),
    harSoknadUnderBehandling: Boolean = false,
    kanSendeInnHelgForMeldekort: Boolean = false,
    jwt: String? = JwtGenerator().createJwtForSystembruker(),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = "Sak lagret",
    forventetContentType: ContentType? = ContentType.Text.Plain.withCharset(Charsets.UTF_8),
): Sak {
    return mottaSakRequest(
        tac = tac,
        requestDto = SakTilMeldekortApiDTO(
            fnr = fnr.verdi,
            sakId = sakId.toString(),
            saksnummer = saksnummer,
            meldeperioder = meldeperioder,
            harSoknadUnderBehandling = harSoknadUnderBehandling,
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
        ),
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    )
}

/**
 * Route: [no.nav.tiltakspenger.meldekort.routes.meldekort.saksbehandling.sakFraSaksbehandlingRoute]
 * Request DTO: [no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO]
 */
suspend fun ApplicationTestBuilder.mottaSakRequest(
    tac: TestApplicationContext,
    requestDto: SakTilMeldekortApiDTO,
    jwt: String? = JwtGenerator().createJwtForSystembruker(),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = "Sak lagret",
    forventetContentType: ContentType? = ContentType.Text.Plain.withCharset(Charsets.UTF_8),
): Sak {
    defaultRequestWithAssertions(
        method = HttpMethod.Post,
        uri = url {
            protocol = URLProtocol.HTTPS
            path("/saksbehandling/sak")
        },
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    ) {
        setBody(serialize(requestDto))
    }
    return tac.sakRepo.hent(SakId.fromString(requestDto.sakId))!!
}
