package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.meldekort.routes.meldekort.MeldekortDagDTO
import java.time.LocalDate

data class MeldekortDag(
    val dag: LocalDate,
    val status: MeldekortDagStatus,
)

fun MeldekortDagDTO.toMeldekortDag(): MeldekortDag {
    return MeldekortDag(
        dag = this.dag,
        status = toMeldekortDagStatus(status),
    )
}

fun List<MeldekortDagDTO>.toMeldekortDager(): List<MeldekortDag> {
    return this.map { it.toMeldekortDag() }
}
