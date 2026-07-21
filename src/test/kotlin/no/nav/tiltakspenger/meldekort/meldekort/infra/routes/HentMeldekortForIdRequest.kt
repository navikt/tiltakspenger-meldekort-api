package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.MeldekortId
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
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): MeldekortTilBrukerDTO? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.Get,
        uri = "/brukerfrontend/meldekort/$meldekortId",
        jwt = jwt,
        forventet = ForventetRespons(
            status = forventetStatus,
            body = forventetBody.tilForventetBody(),
            contentType = forventetContentType,
        ),
    )
    return if (response.status == HttpStatusCode.OK) {
        deserialize<MeldekortTilBrukerDTO>(response.bodyAsText())
    } else {
        null
    }
}
