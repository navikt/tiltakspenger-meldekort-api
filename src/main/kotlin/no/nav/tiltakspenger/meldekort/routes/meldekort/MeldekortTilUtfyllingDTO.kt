package no.nav.tiltakspenger.meldekort.routes.meldekort

import no.nav.tiltakspenger.meldekort.domene.Meldekort
import java.time.LocalDate

data class MeldekortTilUtfyllingDTO(
    val id: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val status: String,
    val meldekortDager: List<MeldekortDagDTO>,
)

data class MeldekortFraUtfyllingDTO(
    val id: String,
    val meldekortDager: List<MeldekortDagDTO>,
)

fun Meldekort.tilUtfyllingDTO(): MeldekortTilUtfyllingDTO {
    return MeldekortTilUtfyllingDTO(
        id = this.id.toString(),
        fraOgMed = this.fraOgMed,
        tilOgMed = this.tilOgMed,
        status = this.status,
        meldekortDager = this.meldekortDager.toDTO(),
    )
}
