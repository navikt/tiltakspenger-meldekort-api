package no.nav.tiltakspenger.meldekort.routes.meldekort

import no.nav.tiltakspenger.meldekort.clients.saksbehandling.MeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.toDTO
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import java.time.LocalDate

data class MeldekortTilUtfyllingDTO(
    val id: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val status: MeldekortStatusDTO,
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
        status = this.status.toDTO(),
        meldekortDager = this.meldekortDager.toDTO(),
    )
}
