package no.nav.tiltakspenger.routes.requests

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.domene.MeldekortTilBrukerDTO
import no.nav.tiltakspenger.routes.defaultRequest

/**
 * Route: [no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.meldekortFraBrukerRoute] - `patch("/{meldekortId}/korriger")`
 * Dto: [no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.MeldekortKorrigertDagDTO]
 */
suspend fun ApplicationTestBuilder.korrigerMeldekort(
    meldekortId: String,
    dager: String,
    locale: String?,
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType = ContentType.Application.Json,
): MeldekortTilBrukerDTO? {
    this.defaultRequest(
        method = io.ktor.http.HttpMethod.Patch,
        uri = "/brukerfrontend/$meldekortId/korriger?locale=$locale",
    ) {
        setBody(dager)
    }.let { response ->
        val bodyAsText = response.bodyAsText()
        val contentType = response.contentType()
        val status = response.status
        val dto = if (status == HttpStatusCode.OK) {
            deserialize<MeldekortTilBrukerDTO>(bodyAsText)
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
}
