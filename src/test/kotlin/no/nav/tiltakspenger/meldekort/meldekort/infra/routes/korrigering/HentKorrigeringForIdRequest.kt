package no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering

import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.MeldekortTilKorrigeringDTO

/**
 * Route: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.hentKorrigeringRoute]
 * Response DTO: [MeldekortTilKorrigeringDTO]
 */
suspend fun ApplicationTestBuilder.hentKorrigeringForIdRequest(
    meldekortId: String,
    fnr: String,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): MeldekortTilKorrigeringDTO? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.Get,
        uri = "/brukerfrontend/korrigering/$meldekortId",
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    )
    return if (response.status == HttpStatusCode.OK) {
        deserialize<MeldekortTilKorrigeringDTO>(response.bodyAsText())
    } else {
        null
    }
}
