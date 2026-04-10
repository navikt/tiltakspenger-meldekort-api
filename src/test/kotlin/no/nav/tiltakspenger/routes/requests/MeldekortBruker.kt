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
import no.nav.tiltakspenger.meldekort.domene.BrukerDTO
import no.nav.tiltakspenger.routes.JwtGenerator
import no.nav.tiltakspenger.routes.defaultRequest

/**
 * Route: [no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.meldekortTilBrukerRoutes]
 * Dto: [no.nav.tiltakspenger.meldekort.domene.BrukerDTO]
 */
suspend fun ApplicationTestBuilder.meldekortBrukerRequest(
    jwt: String? = JwtGenerator().createJwtForUser(),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): BrukerDTO.MedSak? {
    val response = defaultRequest(
        HttpMethod.Get,
        url {
            protocol = URLProtocol.HTTPS
            path("/brukerfrontend/bruker")
        },
        jwt = jwt,
    )
    val bodyAsText = response.bodyAsText()
    val contentType = response.contentType()
    val status = response.status
    withClue(
        "Response details:\n" +
            "Status: $status\n" +
            "Content-Type: $contentType\n" +
            "Body: $bodyAsText",
    ) {
        if (forventetBody == "") {
            contentType shouldBe null
        }
        if (contentType == null) {
            bodyAsText shouldBe ""
        }
        status shouldBe forventetStatus
        if (forventetBody != null) {
            bodyAsText shouldBe forventetBody
        }
        contentType shouldBe forventetContentType
    }
    return if (status == HttpStatusCode.OK) {
        deserialize<BrukerDTO.MedSak>(bodyAsText)
    } else {
        null
    }
}
