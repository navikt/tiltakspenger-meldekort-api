package no.nav.tiltakspenger.meldekort.domene

import java.time.LocalDate

/**
 * Reservert til brukers innsending. Vi må lage en [MeldeperiodeDag] for å støtte opp om [Meldekort]
 */
data class MeldekortDag(
    val dag: LocalDate,
    val status: MeldekortDagStatus,
)
