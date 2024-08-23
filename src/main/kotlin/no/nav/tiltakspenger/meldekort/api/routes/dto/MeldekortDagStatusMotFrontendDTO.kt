package no.nav.tiltakspenger.meldekort.api.routes.dto

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus

enum class MeldekortDagStatusMotFrontendDTO(status: String) {
    SPERRET("sperret"),
    IKKE_UTFYLT("Ikke utfylt"),
    DELTATT_UTEN_LØNN_I_TILTAKET("Deltatt uten lønn i tiltaket"),
    DELTATT_MED_LØNN_I_TILTAKET("Deltatt med lønn i tiltaket"),
    IKKE_DELTATT("Ikke deltatt i tiltaket"),
    FRAVÆR_SYK("Fravær - Syk"),
    FRAVÆR_SYKT_BARN("Fravær - Sykt barn"),
    FRAVÆR_VELFERD_GODKJENT_AV_NAV("Fravær - Velferd. Godkjent av NAV"),
    FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV("Fravær - Velferd. Ikke godkjent av NA"),
}

fun MeldekortDagStatus.toDTO(): MeldekortDagStatusMotFrontendDTO =
    when (this) {
        MeldekortDagStatus.SPERRET -> MeldekortDagStatusMotFrontendDTO.SPERRET
        MeldekortDagStatus.IKKE_UTFYLT -> MeldekortDagStatusMotFrontendDTO.IKKE_UTFYLT
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatusMotFrontendDTO.DELTATT_UTEN_LØNN_I_TILTAKET
        MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatusMotFrontendDTO.DELTATT_MED_LØNN_I_TILTAKET
        MeldekortDagStatus.IKKE_DELTATT -> MeldekortDagStatusMotFrontendDTO.IKKE_DELTATT
        MeldekortDagStatus.FRAVÆR_SYK -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_SYK
        MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_SYKT_BARN
        MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_VELFERD_GODKJENT_AV_NAV
        MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> MeldekortDagStatusMotFrontendDTO.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
    }
