package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortTilBrukerDTO

fun MeldekortForKjedeDTO.shouldBe(
    kjedeId: String? = null,
    periode: PeriodeDTO? = null,
    meldekort: List<MeldekortTilBrukerDTO> = emptyList(),
) {
    this shouldBe MeldekortForKjedeDTO(
        kjedeId = kjedeId,
        periode = periode,
        meldekort = meldekort,
    )
}
