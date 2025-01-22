package no.nav.tiltakspenger.meldekort.routes.meldekort

import no.nav.tiltakspenger.meldekort.clients.saksbehandling.MeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
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
        fraOgMed = this.periode.fraOgMed,
        tilOgMed = this.periode.tilOgMed,
        status = when (this.status) {
            MeldekortStatus.INNSENDT -> MeldekortStatusDTO.INNSENDT
            MeldekortStatus.KAN_UTFYLLES -> MeldekortStatusDTO.KAN_UTFYLLES
            MeldekortStatus.KAN_IKKE_UTFYLLES -> MeldekortStatusDTO.KAN_IKKE_UTFYLLES
        },
        meldekortDager = this.dager.toDTO(),
    )
}
