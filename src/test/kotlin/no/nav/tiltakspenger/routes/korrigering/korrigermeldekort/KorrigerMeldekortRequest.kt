package no.nav.tiltakspenger.routes.korrigering.korrigermeldekort

import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.domene.MeldekortTilBrukerDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR
import no.nav.tiltakspenger.routes.JwtGenerator
import no.nav.tiltakspenger.routes.defaultRequestWithAssertions

/**
 * Route: [no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.korrigering.korrigerMeldekortRoute]
 * Request DTO: [no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.korrigering.MeldekortKorrigertDagDTO]
 * Response DTO: [MeldekortTilBrukerDTO]
 */
suspend fun ApplicationTestBuilder.korrigerMeldekortRequest(
    meldekortId: String,
    requestBody: String,
    locale: String?,
    fnr: String = FAKE_FNR,
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
        deserialize<MeldekortTilBrukerDTO>(response.bodyAsText())
    } else {
        null
    }
}
