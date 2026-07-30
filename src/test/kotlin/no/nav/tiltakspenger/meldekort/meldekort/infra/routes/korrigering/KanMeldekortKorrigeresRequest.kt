package no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator

/**
 * Route: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.kanKorrigeresRoute]
 * Response DTO: [KanKorrigereMeldekortDto]
 */
suspend fun ApplicationTestBuilder.kanMeldekortKorrigeresRequest(
    meldekortId: String,
    fnr: String,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventet: ForventetRespons? = ForventetRespons(200, contentType = "text/plain; charset=UTF-8"),
): KanKorrigereMeldekortDto? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.GET,
        uri = "/brukerfrontend/$meldekortId/kan-korrigeres",
        jwt = jwt,
        forventet = forventet,
    )
    return if (response.statusCode == 200) {
        deserialize<KanKorrigereMeldekortDto>(response.body)
    } else {
        null
    }
}
