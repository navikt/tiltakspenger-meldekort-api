package no.nav.tiltakspenger.meldekort.api.domene

import java.time.LocalDate
import java.util.UUID

data class MeldekortDag(
    val dato: LocalDate,
    val tiltak: Tiltak?,
    val status: MeldekortDagStatus,
    val meldekortId: UUID,
    val løpenr: Int = -1,
) {
    init {
        check(status != MeldekortDagStatus.IKKE_UTFYLT || tiltak == null) {
            "Må ha tiltak hvis status ikke er IKKE_UTFYLT"
        }
    }

    companion object {
        fun lagIkkeUtfyltPeriode(
            meldekortId: UUID,
            fom: LocalDate,
            tom: LocalDate,
            utfallsperioder: List<Utfallsperiode>,
        ): List<MeldekortDag> {
            return fom.datesUntil(tom.plusDays(1)).toList().map { idag ->
                MeldekortDag(
                    dato = idag,
                    tiltak = null,
                    status = utfallsperioder.find { it.fom <= idag && it.tom >= idag }?.let {
                        when (it.utfall) {
                            UtfallForPeriode.GIR_RETT_TILTAKSPENGER -> MeldekortDagStatus.IKKE_UTFYLT
                            UtfallForPeriode.GIR_IKKE_RETT_TILTAKSPENGER -> MeldekortDagStatus.SPERRET
                            UtfallForPeriode.KREVER_MANUELL_VURDERING -> throw IllegalStateException("Skal ikke være mulig å generere meldekort som krever manuelle vurderinger")
                        }
                    } ?: MeldekortDagStatus.SPERRET,
                    meldekortId = meldekortId,
                )
            }
        }
    }
}

enum class MeldekortDagStatus(status: String) {
    SPERRET("Ikke rett på tiltakspenger"),
    IKKE_UTFYLT("Ikke utfylt"),
    DELTATT("Deltatt"),
    IKKE_DELTATT("Ikke deltatt"),
    FRAVÆR_SYK("Fravær syk"),
    FRAVÆR_SYKT_BARN("Fravær sykt barn"),
    FRAVÆR_VELFERD("Fravær velferd"),
    LØNN_FOR_TID_I_ARBEID("Lønn for tid i arbeid"),
    ;
    fun kanSendesInnFraMeldekort(): Boolean = this in listOf(DELTATT, IKKE_DELTATT, FRAVÆR_SYK, FRAVÆR_SYKT_BARN, FRAVÆR_VELFERD, LØNN_FOR_TID_I_ARBEID)
}
