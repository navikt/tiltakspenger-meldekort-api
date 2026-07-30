package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator

/**
 * Route: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.hentMeldekortForKjedeRoute]
 * Response DTO: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.MeldekortForKjedeDTO]
 */
suspend fun ApplicationTestBuilder.hentMeldekortForKjedeIdRequest(
    kjedeId: String,
    fnr: String,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json"),
): MeldekortForKjedeDTO? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.GET,
        uri = "/brukerfrontend/kjede/$kjedeId",
        jwt = jwt,
        forventet = forventet,
    )
    return if (response.statusCode == 200) {
        deserialize<MeldekortForKjedeDTO>(response.body)
    } else {
        null
    }
}
