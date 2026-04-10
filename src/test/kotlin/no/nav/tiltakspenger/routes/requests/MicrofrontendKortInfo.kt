package no.nav.tiltakspenger.routes.requests

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.routes.meldekort.microfrontend.MicrofrontendKortDTO
import no.nav.tiltakspenger.routes.JwtGenerator
import no.nav.tiltakspenger.routes.defaultRequest

/**
 * Route: [no.nav.tiltakspenger.meldekort.routes.meldekort.microfrontend.microfrontendRoutes]
 * Response DTO: [no.nav.tiltakspenger.meldekort.routes.meldekort.microfrontend.MicrofrontendKortDTO]
 */
suspend fun ApplicationTestBuilder.microfrontendKortInfo(
    jwt: String? = JwtGenerator().createJwtForUser(),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType = ContentType.Application.Json,
): MicrofrontendKortDTO? {
    this.defaultRequest(
        method = HttpMethod.Get,
        uri = "/din-side/microfrontend/meldekort-kort-info",
        jwt = jwt,
    ).let { response ->
        val bodyAsText = response.bodyAsText()
        val contentType = response.contentType()
        val status = response.status
        val dto = if (status == HttpStatusCode.OK) {
            deserialize<MicrofrontendKortDTO>(bodyAsText)
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
            if (contentType == null) {
                bodyAsText shouldBe ""
            }
            status shouldBe forventetStatus
            if (forventetBody != null) {
                bodyAsText shouldBe forventetBody
            }
            contentType shouldBe forventetContentType
        }
        return dto
    }
}
