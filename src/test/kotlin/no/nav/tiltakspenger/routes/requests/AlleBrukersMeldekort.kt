package no.nav.tiltakspenger.routes.requests

import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.routes.defaultRequest
import org.json.JSONObject

suspend fun ApplicationTestBuilder.alleBrukersMeldekort(): String {
    this.defaultRequest(
        method = HttpMethod.Get,
        uri = "/brukerfrontend/meldekort/alle",
    ).let { response ->
        return response.bodyAsText()
    }
}

fun ApplicationTestBuilder.verifiserKunEtMeldekortFraAlleMeldekort(alleMeldekort: String) {
    JSONObject(alleMeldekort).getJSONArray("meldekort").single()
}

fun String.hentFørsteMeldekortFraAlleMeldekort(): String =
    JSONObject(this).getJSONArray("meldekort").getJSONObject(0).toString()
