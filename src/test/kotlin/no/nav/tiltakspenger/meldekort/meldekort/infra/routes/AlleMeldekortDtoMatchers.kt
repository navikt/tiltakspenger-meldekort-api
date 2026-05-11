package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.meldekort.bruker.infra.routes.BrukerDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.AlleMeldekortDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortMedSisteMeldeperiodeDTO

fun AlleMeldekortDTO.shouldBe(
    bruker: BrukerDTO,
    meldekortMedSisteMeldeperiode: List<MeldekortMedSisteMeldeperiodeDTO> = emptyList(),
) {
    this shouldBe AlleMeldekortDTO(
        bruker = bruker,
        meldekortMedSisteMeldeperiode = meldekortMedSisteMeldeperiode,
    )
}
