package no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.jobber.KjørJobberForTester
import no.nav.tiltakspenger.meldekort.infra.routes.tilForventetBody
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortTilBrukerDTO

/**
 * Route: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.korrigerMeldekortRoute]
 * Request DTO: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.MeldekortKorrigertDagDTO]
 * Response DTO: [MeldekortTilBrukerDTO]
 */
suspend fun ApplicationTestBuilder.korrigerMeldekortRequest(
    tac: TestApplicationContext,
    meldekortId: String,
    requestDto: List<MeldekortKorrigertDagDTO>,
    locale: String?,
    fnr: String,
    runJobs: Boolean = true,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json"),
): MeldekortTilBrukerDTO? {
    return korrigerMeldekortRequest(
        tac = tac,
        meldekortId = meldekortId,
        requestBody = serialize(requestDto),
        locale = locale,
        fnr = fnr,
        runJobs = runJobs,
        jwt = jwt,
        forventet = forventet,
    )
}

/**
 * Route: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.korrigerMeldekortRoute]
 * Request DTO: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.MeldekortKorrigertDagDTO]
 * Response DTO: [MeldekortTilBrukerDTO]
 */
suspend fun ApplicationTestBuilder.korrigerMeldekortRequest(
    tac: TestApplicationContext,
    meldekortId: String,
    requestBody: String,
    locale: String?,
    fnr: String,
    runJobs: Boolean = true,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json"),
): MeldekortTilBrukerDTO? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.PATCH,
        uri = "/brukerfrontend/$meldekortId/korriger?locale=$locale",
        jwt = jwt,
        forventet = forventet,
        body = requestBody,
    )
    return if (response.statusCode == 200) {
        if (runJobs) {
            KjørJobberForTester.kjørVarsler(tac)
        }
        deserialize<MeldekortTilBrukerDTO>(response.body)
    } else {
        null
    }
}
