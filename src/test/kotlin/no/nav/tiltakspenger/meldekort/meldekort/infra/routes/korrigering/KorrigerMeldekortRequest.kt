package no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering

import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.infra.routes.jobber.KjørJobberForTester
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
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): MeldekortTilBrukerDTO? {
    return korrigerMeldekortRequest(
        tac = tac,
        meldekortId = meldekortId,
        requestBody = serialize(requestDto),
        locale = locale,
        fnr = fnr,
        runJobs = runJobs,
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
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
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): MeldekortTilBrukerDTO? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.Patch,
        uri = "/brukerfrontend/$meldekortId/korriger?locale=$locale",
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    ) {
        setBody(requestBody)
    }
    return if (response.status == HttpStatusCode.OK) {
        if (runJobs) {
            KjørJobberForTester.kjørVarsler(tac)
        }
        deserialize<MeldekortTilBrukerDTO>(response.bodyAsText())
    } else {
        null
    }
}
