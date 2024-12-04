package no.nav.tiltakspenger.meldekort.domene

import java.time.LocalDate

enum class MeldekortDagStatus {
    Deltatt,
    FraværSyk,
    FraværSyktBarn,
    FraværAnnet,
    IkkeDeltatt,
    IkkeRegistrert,
}

data class MeldekortDag(
    val dato: LocalDate,
    val status: MeldekortDagStatus,
)
