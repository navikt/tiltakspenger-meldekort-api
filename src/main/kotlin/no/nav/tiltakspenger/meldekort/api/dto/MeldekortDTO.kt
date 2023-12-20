package no.nav.tiltakspenger.meldekort.api.dto

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import no.nav.tiltakspenger.meldekort.api.domene.Tiltak
import no.nav.tiltakspenger.meldekort.api.routes.TiltakDTO
import java.time.LocalDate
import java.util.UUID

// data class Tiltak(
//    val tiltakTypekode: String,
//    val tiltakTypeBeskrivelse: String,
//    val antallDagerPåTiltaket: Number,
//    val fom: LocalDate,
//    val tom: LocalDate,
// )

enum class MeldekortStatus(status: String) {
    IKKE_UTFYLT("Ikke utfylt"),
    DELTATT("Deltatt"),
    IKKE_DELTATT("Ikke deltatt"),
    FRAVÆR_SYK("Fravær syk"),
    FRAVÆR_SYKT_BARN("Fravær sykt barn"),
    FRAVÆR_VELFERD("Fravær velferd"),
    LØNN_FOR_TID_I_ARBEID("Lønn for tid i arbeid"),
}

data class MeldekortDag(
    val dato: LocalDate,
    val tiltak: Tiltak?,
    val status: MeldekortStatus,
) {
    init {
        check(status != MeldekortStatus.IKKE_UTFYLT || tiltak == null) {
            "Må ha tiltak hvis status ikke er IKKE_UTFYLT"
        }
    }

    companion object {
        fun lagIkkeUtfyltPeriode(fom: LocalDate, tom: LocalDate): List<MeldekortDag> {
            return fom.datesUntil(tom.plusDays(1)).toList().map {
                MeldekortDag(
                    dato = it,
                    tiltak = null,
                    status = MeldekortStatus.IKKE_UTFYLT,
                )
            }
        }
    }
}

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

data class MeldekortBehandling(
    val id: UUID,
    val meldekort: List<Meldekort>,
    val antallDagerDeltatt: Int,
    val antallDagerIkkeDeltatt: Int,
    val antDager75Prosent: Int,
    val antDager100Prosent: Int,
)

// data class MeldekortDTO(
//    val id: String,
//    val fom: LocalDate,
//    val tom: LocalDate,
//    val tiltak: Tiltak,
//    val meldekortUke1: List<MeldekortDag>,
//    val meldekortUke2: List<MeldekortDag>,
//    val sendtInnDato: LocalDate?,
// )

data class MeldekortDTO(
    val grunnlagsData: MeldekortGrunnlag,
    val meldekort: List<Meldekort>,
)

data class MeldekortMedTiltak(
    val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val tiltak: List<TiltakDTO>,
    val meldekortUke1: List<MeldekortDag>,
    val meldekortUke2: List<MeldekortDag>,
)

data class MeldekortDTOTest(
    val id: String,
)
