package no.nav.tiltakspenger.meldekort.domene

import java.time.LocalDate

data class MeldekortDag(
    val dag: LocalDate,
    val status: MeldekortDagStatus,
) {
    /** Regner ikke med IKKE_BESVART, IKKE_RETT_TIL_TILTAKSPENGER eller IKKE_TILTAKSDAG som besvart. */
    val erBesvart: Boolean = status !in listOf(
        MeldekortDagStatus.IKKE_BESVART,
        MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
        MeldekortDagStatus.IKKE_TILTAKSDAG,
    )
}
