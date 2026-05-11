package no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering

import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.KanKorrigereMeldekortDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR

/**
 * Route: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.kanKorrigeresRoute]
 * Response DTO: [KanKorrigereMeldekortDto]
 */
suspend fun ApplicationTestBuilder.kanMeldekortKorrigeresRequest(
    meldekortId: String,
    fnr: String = FAKE_FNR,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Text.Plain.withCharset(Charsets.UTF_8),
): KanKorrigereMeldekortDto? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.Get,
        uri = "/brukerfrontend/$meldekortId/kan-korrigeres",
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    )
    return if (response.status == HttpStatusCode.OK) {
        deserialize<KanKorrigereMeldekortDto>(response.bodyAsText())
    } else {
        null
    }
}
