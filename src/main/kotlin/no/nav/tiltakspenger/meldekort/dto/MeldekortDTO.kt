package no.nav.tiltakspenger.meldekort.dto

import java.time.LocalDate

data class Tiltak(
    val tiltakType: String,
    val antallDagerPåTiltaket: Number
)

enum class MeldekortStatus(status: String) {
    DELTATT("Deltatt"),
    IKKE_DELTATT("Ikke deltatt"),
    FRAVÆR_SYK("Fravær syk"),
    FRAVÆR_SYKT_BARN("Fravær sykt barn"),
    FRAVÆR_VELFERD("Fravær velferd"),
    LØNN_FOR_TID_I_ARBEID("Lønn for tid i arbeid"),
}

data class MeldekortDag(
    val dag: String,
    val dato: LocalDate,
    val status: MeldekortStatus,
)

data class MeldekortDTO (
    val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val tiltak: Tiltak,
    val meldekortUke1: List<MeldekortDag>,
    val meldekortUke2: List<MeldekortDag>,
    val sendtInnDato: LocalDate,
)

data class MeldekortDTOTest (
    val id: String
)
