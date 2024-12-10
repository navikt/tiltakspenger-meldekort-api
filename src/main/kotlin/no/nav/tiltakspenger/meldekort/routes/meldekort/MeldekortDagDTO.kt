package no.nav.tiltakspenger.meldekort.routes.meldekort

import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import java.time.LocalDate

data class MeldekortDagDTO(
    val dag: LocalDate,
    val status: String,
)

fun List<MeldekortDag>.toDTO(): List<MeldekortDagDTO> =
    this.map { dag ->
        MeldekortDagDTO(dag = dag.dag, status = dag.status.name)
    }
