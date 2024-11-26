package no.nav.tiltakspenger.meldekort.domene

import java.time.LocalDate

data class MeldekortDager(
    val meldekortDager: List<MeldekortDag>,
)

// TODO Kew: Fyll ut med mer
data class MeldekortDag(
    val dag: LocalDate,
    val status: String,
)
