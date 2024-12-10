package no.nav.tiltakspenger.meldekort.routes.meldekort

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import java.time.LocalDate

// TODO: WIP, hent fra libs etter hvert

enum class MeldekortStatusTilBrukerDTO {
    KAN_UTFYLLES,
    KAN_IKKE_UTFYLLES,
    GODKJENT,
}

data class MeldekortDagTilBrukerDTO(
    val dag: LocalDate,
    val status: MeldekortDagStatus,
)

data class MeldekortTilBrukerDTO(
    val id: String,
    val fnr: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val status: MeldekortStatusTilBrukerDTO,
    val meldekortDager: List<MeldekortDagTilBrukerDTO>,
) {
    fun tilMeldekort(): Meldekort {
        return Meldekort(
            id = MeldekortId.fromString(this.id),
            fnr = Fnr.fromString(this.fnr),
            fraOgMed = this.fraOgMed,
            tilOgMed = this.tilOgMed,
            meldeperiodeId = MeldeperiodeId("$fraOgMed/$tilOgMed"),
            status = when (this.status) {
                MeldekortStatusTilBrukerDTO.GODKJENT -> MeldekortStatus.GODKJENT
                MeldekortStatusTilBrukerDTO.KAN_UTFYLLES -> MeldekortStatus.KAN_UTFYLLES
                MeldekortStatusTilBrukerDTO.KAN_IKKE_UTFYLLES -> MeldekortStatus.KAN_IKKE_UTFYLLES
            },
            meldekortDager = this.meldekortDager.map {
                MeldekortDag(
                    dag = it.dag,
                    status = it.status,
                )
            },
        )
    }
}
