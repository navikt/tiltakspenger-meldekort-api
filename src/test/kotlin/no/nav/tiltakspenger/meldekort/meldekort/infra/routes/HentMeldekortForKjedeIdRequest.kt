package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.tilForventetBody

/**
 * Route: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.hentMeldekortForKjedeRoute]
 * Response DTO: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.MeldekortForKjedeDTO]
 */
suspend fun ApplicationTestBuilder.hentMeldekortForKjedeIdRequest(
    kjedeId: String,
    fnr: String,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): MeldekortForKjedeDTO? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.Get,
        uri = "/brukerfrontend/kjede/$kjedeId",
        jwt = jwt,
        forventet = ForventetRespons(
            status = forventetStatus,
            body = forventetBody.tilForventetBody(),
            contentType = forventetContentType,
        ),
    )
    return if (response.status == HttpStatusCode.OK) {
        deserialize<MeldekortForKjedeDTO>(response.bodyAsText())
    } else {
        null
    }
}
