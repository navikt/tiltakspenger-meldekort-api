package no.nav.tiltakspenger.meldekort.mottak.infra.routes

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.jobber.KjørJobberForTester
import no.nav.tiltakspenger.meldekort.infra.routes.tilForventetBody
import no.nav.tiltakspenger.meldekort.sak.Sak
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto

/**
 * Route: [no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottakFraSaksbehandlingRoute]
 * Request DTO: [no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO]
 */
suspend fun ApplicationTestBuilder.mottaSakRequest(
    tac: TestApplicationContext,
    fnr: Fnr = tac.nesteFnr(),
    sakId: SakId = SakId.random(),
    saksnummer: String = tac.nesteSaksnummer(),
    meldeperioder: List<SakTilMeldekortApiDTO.MeldeperiodeDTO> = listOf(meldeperiodeDto(opprettet = nå(tac.clock))),
    harSoknadUnderBehandling: Boolean = false,
    kanSendeInnHelgForMeldekort: Boolean = false,
    runJobs: Boolean = true,
    jwt: String? = JwtGenerator().createJwtForSystembruker(),
    forventet: ForventetRespons? = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
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
        runJobs = runJobs,
        jwt = jwt,
        forventet = forventet,
    )
}

/**
 * Route: [no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottakFraSaksbehandlingRoute]
 * Request DTO: [no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO]
 */
suspend fun ApplicationTestBuilder.mottaSakRequest(
    tac: TestApplicationContext,
    requestDto: SakTilMeldekortApiDTO,
    runJobs: Boolean = true,
    jwt: String? = JwtGenerator().createJwtForSystembruker(),
    forventet: ForventetRespons? = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
): Sak {
    defaultRequestWithAssertions(
        method = HttpMethod.POST,
        uri = "/saksbehandling/sak",
        jwt = jwt,
        forventet = forventet,
        body = serialize(requestDto),
    )
    if (runJobs && forventet?.status == 200) {
        KjørJobberForTester.kjørVarsler(tac)
    }
    return tac.sakRepo.hent(SakId.fromString(requestDto.sakId))!!
}
