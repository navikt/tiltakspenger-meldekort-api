package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.tilForventetBody
import no.nav.tiltakspenger.meldekort.meldekort.infra.AlleMeldekortDTO

typealias JsonResponse = String

/**
 * Route: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.hentInnsendteMeldekortRoute]
 * Response DTO: [AlleMeldekortDTO]
 *
 * Returnerer rå JSON.
 * Tester som trenger domeneverdier bør hente dem via repo/service i testen, mens wire-formatet kan sjekkes med [shouldBeAlleMeldekortJson].
 */
suspend fun ApplicationTestBuilder.hentAlleInnsendteMeldekortRequest(
    fnr: String,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): JsonResponse? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.Get,
        uri = "/brukerfrontend/meldekort/innsendte",
        jwt = jwt,
        forventet = ForventetRespons(
            status = forventetStatus,
            body = forventetBody.tilForventetBody(),
            contentType = forventetContentType,
        ),
    )
    return if (response.status == forventetStatus) response.bodyAsText() else null
}
