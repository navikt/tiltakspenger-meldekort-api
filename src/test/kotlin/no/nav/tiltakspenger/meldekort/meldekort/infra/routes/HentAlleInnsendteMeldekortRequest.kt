package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
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
    forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json"),
): JsonResponse? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.GET,
        uri = "/brukerfrontend/meldekort/innsendte",
        jwt = jwt,
        forventet = forventet,
    )
    return if (response.statusCode == forventet?.status) response.body else null
}
