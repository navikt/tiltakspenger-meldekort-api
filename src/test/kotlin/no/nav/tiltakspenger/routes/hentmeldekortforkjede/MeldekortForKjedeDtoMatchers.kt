package no.nav.tiltakspenger.routes.hentmeldekortforkjede

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.MeldekortForKjedeDTO

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
