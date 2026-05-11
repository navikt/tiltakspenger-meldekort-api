package no.nav.tiltakspenger.meldekort.landingsside.infra.routes

import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.defaultRequestWithAssertions
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR

/**
 * Route: [no.nav.tiltakspenger.meldekort.landingsside.infra.routes.fellesLandingssideRoutes]
 * Response DTO: [LandingssideStatusDTO]
 */
suspend fun ApplicationTestBuilder.landingssideStatusRequest(
    fnr: String = FAKE_FNR,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): LandingssideStatusDTO? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.Get,
        uri = url {
            protocol = URLProtocol.HTTPS
            path("/landingsside/status")
        },
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    )
    return if (response.status == HttpStatusCode.OK) {
        deserialize<LandingssideStatusDTO>(response.bodyAsText())
    } else {
        null
    }
}
