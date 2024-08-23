package no.nav.tiltakspenger.meldekort.api.routes.dto

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDag
import java.time.LocalDate

data class MeldekortDagDTO(
    val meldekortId: String,
    val dato: LocalDate,
    val status: MeldekortDagStatusMotFrontendDTO,
)

fun List<MeldekortDag>.toDTO(): List<MeldekortDagDTO> {
    return this.map {
        MeldekortDagDTO(
            meldekortId = it.meldekortId.toString(),
            dato = it.dato,
            status = it.status.toDTO(),
        )
    }
}
