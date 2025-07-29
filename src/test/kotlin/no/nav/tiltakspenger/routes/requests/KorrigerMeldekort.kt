package no.nav.tiltakspenger.routes.requests

import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.domene.MeldekortTilBrukerDTO
import no.nav.tiltakspenger.routes.defaultRequest

suspend fun ApplicationTestBuilder.korrigerMeldekort(
    meldekortId: String,
    dager: String,
): Pair<MeldekortTilBrukerDTO, String> {
    this.defaultRequest(
        method = io.ktor.http.HttpMethod.Patch,
        uri = "/brukerfrontend/$meldekortId/korriger",
    ) {
        setBody(dager)
    }.let { response ->
        response.status shouldBe HttpStatusCode.OK
        val body = response.bodyAsText()
        return deserialize<MeldekortTilBrukerDTO>(body) to body
    }
}
