package no.nav.tiltakspenger.routes.hentinnsendtemeldekort

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.meldekort.domene.AlleMeldekortDTO
import no.nav.tiltakspenger.meldekort.domene.BrukerDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortMedSisteMeldeperiodeDTO

fun AlleMeldekortDTO.shouldBe(
    bruker: BrukerDTO,
    meldekortMedSisteMeldeperiode: List<MeldekortMedSisteMeldeperiodeDTO> = emptyList(),
) {
    this shouldBe AlleMeldekortDTO(
        bruker = bruker,
        meldekortMedSisteMeldeperiode = meldekortMedSisteMeldeperiode,
    )
}
