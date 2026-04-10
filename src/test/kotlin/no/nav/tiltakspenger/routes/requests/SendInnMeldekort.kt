package no.nav.tiltakspenger.routes.requests

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.routes.JwtGenerator
import no.nav.tiltakspenger.routes.defaultRequest

/**
 * Route: [no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.sendInnMeldekortRoute]
 * Request DTO: [no.nav.tiltakspenger.meldekort.domene.MeldekortFraBrukerDTO]
 */
suspend fun ApplicationTestBuilder.sendInnMeldekort(
    requestBody: String,
    jwt: String? = JwtGenerator().createJwtForUser(),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): String {
    this.defaultRequest(
        method = HttpMethod.Post,
        uri = "/brukerfrontend/send-inn",
        jwt = jwt,
    ) {
        setBody(requestBody)
    }.let { response ->
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
        return bodyAsText
    }
}
