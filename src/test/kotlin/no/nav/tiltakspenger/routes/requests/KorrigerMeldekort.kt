package no.nav.tiltakspenger.routes.requests

import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.routes.defaultRequest

suspend fun ApplicationTestBuilder.korrigerMeldekort(
    meldekortId: String,
    dager: String,
    locale: String?,
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
): String {
    this.defaultRequest(
        method = io.ktor.http.HttpMethod.Patch,
        uri = "/brukerfrontend/$meldekortId/korriger?locale=$locale",
    ) {
        setBody(dager)
    }.let { response ->
        response.status shouldBe forventetStatus
        val body = response.bodyAsText()
        return body
    }
}
