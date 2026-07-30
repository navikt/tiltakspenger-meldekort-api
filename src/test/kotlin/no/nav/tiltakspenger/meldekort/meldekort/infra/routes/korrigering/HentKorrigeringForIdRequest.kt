package no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.MeldekortTilKorrigeringDTO

/**
 * Route: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.hentKorrigeringRoute]
 * Response DTO: [MeldekortTilKorrigeringDTO]
 */
suspend fun ApplicationTestBuilder.hentKorrigeringForIdRequest(
    meldekortId: String,
    fnr: String,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json"),
): MeldekortTilKorrigeringDTO? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.GET,
        uri = "/brukerfrontend/korrigering/$meldekortId",
        jwt = jwt,
        forventet = forventet,
    )
    return if (response.statusCode == 200) {
        deserialize<MeldekortTilKorrigeringDTO>(response.body)
    } else {
        null
    }
}
