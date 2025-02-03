package no.nav.tiltakspenger.meldekort.domene

import java.time.LocalDate

interface MeldekortDag {
    val dag: LocalDate
    val status: MeldekortDagStatus
}

data class BrukersMeldekortDag(
    override val dag: LocalDate,
    override val status: MeldekortDagStatus,
    val harRett: Boolean,
) : MeldekortDag
