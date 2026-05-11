package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.meldekort.infra.AlleMeldekortDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR

/**
 * Route: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.hentInnsendteMeldekortRoute]
 * Response DTO: [AlleMeldekortDTO]
 */
suspend fun ApplicationTestBuilder.hentAlleInnsendteMeldekortRequest(
    fnr: String = FAKE_FNR,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): AlleMeldekortDTO? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.Get,
        uri = "/brukerfrontend/meldekort/innsendte",
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    )
    return if (response.status == HttpStatusCode.OK) {
        deserialize<AlleMeldekortDTO>(response.bodyAsText())
    } else {
        null
    }
}
