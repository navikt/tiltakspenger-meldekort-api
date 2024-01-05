package no.nav.tiltakspenger.meldekort.api.domene

import java.time.LocalDate
import java.util.*

sealed interface Meldekort {
    val id: UUID
    val fom: LocalDate
    val tom: LocalDate
    val meldekortDager: List<MeldekortDag>

    data class Åpent(
        override val id: UUID,
        override val fom: LocalDate,
        override val tom: LocalDate,
        override val meldekortDager: List<MeldekortDag>,
    ) : Meldekort
    data class Innsendt(
        override val id: UUID,
        override val fom: LocalDate,
        override val tom: LocalDate,
        override val meldekortDager: List<MeldekortDag>,
        val sendtInnDato: LocalDate,
    ) : Meldekort
}
