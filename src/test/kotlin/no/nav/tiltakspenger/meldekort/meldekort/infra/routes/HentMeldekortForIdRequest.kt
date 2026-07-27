package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.tilForventetBody
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortTilBrukerDTO

/**
 * Route: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.hentMeldekortRoute]
 * Response DTO: [MeldekortTilBrukerDTO]
 */
suspend fun ApplicationTestBuilder.hentMeldekortForIdRequest(
    meldekortId: MeldekortId,
    fnr: String,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json"),
): MeldekortTilBrukerDTO? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.GET,
        uri = "/brukerfrontend/meldekort/$meldekortId",
        jwt = jwt,
        forventet = forventet,
    )
    return if (response.statusCode == 200) {
        deserialize<MeldekortTilBrukerDTO>(response.body)
    } else {
        null
    }
}
