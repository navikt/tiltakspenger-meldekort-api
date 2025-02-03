package no.nav.tiltakspenger.meldekort.domene

import java.time.LocalDate

data class BrukersMeldekortDag(
    val dag: LocalDate,
    val harRett: Boolean,
    val status: MeldekortDagStatus,
)
