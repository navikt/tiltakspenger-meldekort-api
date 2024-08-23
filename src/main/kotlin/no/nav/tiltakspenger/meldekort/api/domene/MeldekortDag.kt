package no.nav.tiltakspenger.meldekort.api.domene

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.FRAVÆR_SYK
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.FRAVÆR_SYKT_BARN
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.IKKE_DELTATT
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.IKKE_UTFYLT
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.SPERRET
import no.nav.tiltakspenger.meldekort.api.routes.dto.MeldekortDagStatusMotFrontendDTO
import java.time.LocalDate
import java.util.UUID

data class MeldekortDag(
    val dato: LocalDate,
    val tiltak: Tiltak,
    val status: MeldekortDagStatus,
    val meldekortId: UUID,
    val løpenr: Int = -1,
) {
    companion object {
        fun lagIkkeUtfyltPeriode(
            meldekortId: UUID,
            fom: LocalDate,
            tom: LocalDate,
            utfallsperioder: List<Utfallsperiode>,
            tiltak: Tiltak,
        ): List<MeldekortDag> =
            fom.datesUntil(tom.plusDays(1)).toList().map { idag ->
                MeldekortDag(
                    dato = idag,
                    tiltak = tiltak,
                    status =
                    utfallsperioder.find { it.fom <= idag && it.tom >= idag }?.let {
                        when (it.utfall) {
                            UtfallForPeriode.GIR_RETT_TILTAKSPENGER -> MeldekortDagStatus.IKKE_UTFYLT
                            UtfallForPeriode.GIR_IKKE_RETT_TILTAKSPENGER -> MeldekortDagStatus.SPERRET
                        }
                    } ?: MeldekortDagStatus.SPERRET,
                    meldekortId = meldekortId,
                )
            }
    }
}

enum class MeldekortDagStatus(
    status: String,
) {
    SPERRET("sperret"),
    IKKE_UTFYLT("Ikke utfylt"),
    DELTATT_UTEN_LØNN_I_TILTAKET("Deltatt uten lønn i tiltaket"),
    DELTATT_MED_LØNN_I_TILTAKET("Deltatt med lønn i tiltaket"),
    IKKE_DELTATT("Ikke deltatt i tiltaket"),
    FRAVÆR_SYK("Fravær - Syk"),
    FRAVÆR_SYKT_BARN("Fravær - Sykt barn"),
    FRAVÆR_VELFERD_GODKJENT_AV_NAV("Fravær - Velferd. Godkjent av NAV"),
    FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV("Fravær - Velferd. Ikke godkjent av NA"),
    ;

    fun kanSendesInnFraMeldekort(): Boolean =
        this in listOf(
            DELTATT_UTEN_LØNN_I_TILTAKET,
            DELTATT_MED_LØNN_I_TILTAKET,
            IKKE_DELTATT,
            FRAVÆR_SYK,
            FRAVÆR_SYKT_BARN,
            FRAVÆR_VELFERD_GODKJENT_AV_NAV,
            FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV,
        )
}

fun MeldekortDagStatusMotFrontendDTO.toDomain(): MeldekortDagStatus =
    when (this) {
        MeldekortDagStatusMotFrontendDTO.SPERRET -> SPERRET
        MeldekortDagStatusMotFrontendDTO.IKKE_UTFYLT -> IKKE_UTFYLT
        MeldekortDagStatusMotFrontendDTO.DELTATT_UTEN_LØNN_I_TILTAKET -> DELTATT_UTEN_LØNN_I_TILTAKET
        MeldekortDagStatusMotFrontendDTO.DELTATT_MED_LØNN_I_TILTAKET -> DELTATT_MED_LØNN_I_TILTAKET
        MeldekortDagStatusMotFrontendDTO.IKKE_DELTATT -> IKKE_DELTATT
        MeldekortDagStatusMotFrontendDTO.FRAVÆR_SYK -> FRAVÆR_SYK
        MeldekortDagStatusMotFrontendDTO.FRAVÆR_SYKT_BARN -> FRAVÆR_SYKT_BARN
        MeldekortDagStatusMotFrontendDTO.FRAVÆR_VELFERD_GODKJENT_AV_NAV -> FRAVÆR_VELFERD_GODKJENT_AV_NAV
        MeldekortDagStatusMotFrontendDTO.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
    }
