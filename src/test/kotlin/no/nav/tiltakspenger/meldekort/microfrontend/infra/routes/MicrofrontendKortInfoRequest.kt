package no.nav.tiltakspenger.meldekort.microfrontend.infra.routes

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.tilForventetBody
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendMeldekortInfo

/**
 * Route: [no.nav.tiltakspenger.meldekort.microfrontend.infra.routes.microfrontendRoutes]
 */
suspend fun ApplicationTestBuilder.microfrontendKortInfoRequest(
    fnr: String,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json"),
): MicrofrontendMeldekortInfo? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.GET,
        uri = "/din-side/microfrontend/meldekort-kort-info",
        jwt = jwt,
        forventet = forventet,
    )
    return if (response.statusCode == 200) {
        deserialize<MicrofrontendMeldekortInfo>(response.body)
    } else {
        null
    }
}
