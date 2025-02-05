package no.nav.tiltakspenger.meldekort.domene

import java.time.LocalDate

interface IMeldekortDag {
    val dag: LocalDate
    val status: MeldekortDagStatus
}

data class MeldekortDag(
    override val dag: LocalDate,
    override val status: MeldekortDagStatus,
) : IMeldekortDag
