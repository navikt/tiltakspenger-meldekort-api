package no.nav.tiltakspenger.routes.requests

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside.LandingssideStatusDTO
import no.nav.tiltakspenger.routes.JwtGenerator
import no.nav.tiltakspenger.routes.defaultRequest

/**
 * Route: [no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside.fellesLandingssideRoutes]
 * Dto: [no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside.LandingssideStatusDTO]
 */
suspend fun ApplicationTestBuilder.landingssideStatusRequest(
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): LandingssideStatusDTO? {
    val response = defaultRequest(
        HttpMethod.Get,
        url {
            protocol = URLProtocol.HTTPS
            path("/landingsside/status")
        },
        jwt = JwtGenerator().createJwtForUser(),
    )
    val bodyAsText = response.bodyAsText()
    val contentType = response.contentType()
    val status = response.status
    val dto = if (status == HttpStatusCode.OK) {
        deserialize<LandingssideStatusDTO>(bodyAsText)
    } else {
        null
    }
    withClue(
        "Response details:\n" +
            "Status: $status\n" +
            "Content-Type: $contentType\n" +
            "Body: $bodyAsText",
    ) {
        if (forventetBody == "") {
            contentType shouldBe null
        }
        status shouldBe forventetStatus
        if (forventetBody != null) {
            bodyAsText shouldBe forventetBody
        }
        contentType shouldBe forventetContentType
    }
    return dto
}
