package no.nav.tiltakspenger.meldekort.domene

import java.time.LocalDate

data class MeldekortDag(
    val dag: LocalDate,
    val status: String,
)
