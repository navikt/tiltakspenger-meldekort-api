package no.nav.tiltakspenger.routes.matchers

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortTilBrukerDTO

fun MeldekortTilBrukerDTO.harAntallDager(antall: Int): MeldekortTilBrukerDTO {
    dager.size shouldBe antall
    return this
}

fun MeldekortTilBrukerDTO.harKjedeId(forventetKjedeId: MeldeperiodeKjedeId): MeldekortTilBrukerDTO {
    kjedeId shouldBe forventetKjedeId.toString()
    return this
}

fun MeldekortTilBrukerDTO.erInnsendt(): MeldekortTilBrukerDTO {
    innsendt shouldNotBe null
    status shouldBe MeldekortStatusDTO.INNSENDT
    return this
}
