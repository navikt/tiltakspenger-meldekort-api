package no.nav.tiltakspenger.meldekort.api.domene

import java.time.LocalDate

data class MeldekortDag(
    val dato: LocalDate,
    val tiltak: Tiltak?,
    val status: MeldekortDagStatus,
) {
    init {
        check(status != MeldekortDagStatus.IKKE_UTFYLT || tiltak == null) {
            "Må ha tiltak hvis status ikke er IKKE_UTFYLT"
        }
    }

    companion object {
        fun lagIkkeUtfyltPeriode(fom: LocalDate, tom: LocalDate): List<MeldekortDag> {
            return fom.datesUntil(tom.plusDays(1)).toList().map {
                MeldekortDag(
                    dato = it,
                    tiltak = null,
                    status = MeldekortDagStatus.IKKE_UTFYLT,
                )
            }
        }
    }
}

enum class MeldekortDagStatus(status: String) {
    IKKE_UTFYLT("Ikke utfylt"),
    DELTATT("Deltatt"),
    IKKE_DELTATT("Ikke deltatt"),
    FRAVÆR_SYK("Fravær syk"),
    FRAVÆR_SYKT_BARN("Fravær sykt barn"),
    FRAVÆR_VELFERD("Fravær velferd"),
    LØNN_FOR_TID_I_ARBEID("Lønn for tid i arbeid"),
}
