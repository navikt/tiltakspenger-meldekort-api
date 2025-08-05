package no.nav.tiltakspenger.routes.requests

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.routes.defaultRequest

suspend fun ApplicationTestBuilder.alleBrukersMeldekort(): String {
    this.defaultRequest(
        method = HttpMethod.Get,
        uri = "/brukerfrontend/meldekort/alle",
    ).let { response ->
        return response.bodyAsText()
    }
}

fun ApplicationTestBuilder.verifiserKunEtMeldekortFraAlleMeldekort(alleMeldekort: String) {
    jacksonObjectMapper().readTree(alleMeldekort).get("meldekort").single()
}

fun String.verifiserAntallMeldekortFraAlleMeldekort(antall: Int) {
    jacksonObjectMapper().readTree(this).get("meldekort").size() shouldBe antall
}

fun String.hentFørsteMeldekortFraAlleMeldekort(): String =
    jacksonObjectMapper().readTree(this).get("meldekort").first().toString()

fun String.hentSisteMeldekortFraAlleMeldekort(): String = jacksonObjectMapper().readTree(this).get("meldekort").last().toString()
