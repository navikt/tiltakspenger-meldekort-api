package no.nav.tiltakspenger.routes.requests

import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.meldekort.domene.AlleMeldekortDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortTilBrukerDTO
import no.nav.tiltakspenger.routes.defaultRequest
import tools.jackson.module.kotlin.readValue

suspend fun ApplicationTestBuilder.alleBrukersMeldekort(): String {
    this.defaultRequest(
        method = HttpMethod.Get,
        uri = "/brukerfrontend/meldekort/innsendte",
    ).let { response ->
        return response.bodyAsText()
    }
}

fun verifiserKunEtMeldekortFraAlleMeldekort(alleMeldekort: String) {
    alleMeldekort.tilAlleMeldekortDTO().meldekortMedSisteMeldeperiode.single()
}

fun String.verifiserAntallMeldekortFraAlleMeldekort(antall: Int) {
    this.tilAlleMeldekortDTO().meldekortMedSisteMeldeperiode.size shouldBe antall
}

fun String.hentFørsteMeldekortFraAlleMeldekort(): MeldekortTilBrukerDTO =
    this.tilAlleMeldekortDTO().meldekortMedSisteMeldeperiode.first().meldekort

fun String.hentSisteMeldekortFraAlleMeldekort(): MeldekortTilBrukerDTO =
    this.tilAlleMeldekortDTO().meldekortMedSisteMeldeperiode.last().meldekort

fun String.tilAlleMeldekortDTO(): AlleMeldekortDTO {
    return objectMapper.readValue<AlleMeldekortDTO>(this)
}
