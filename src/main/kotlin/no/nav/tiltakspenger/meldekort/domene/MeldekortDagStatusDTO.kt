package no.nav.tiltakspenger.meldekort.domene

enum class MeldekortDagStatusDTO {
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_GODKJENT_AV_NAV,
    FRAVÆR_ANNET,
    IKKE_BESVART,
    IKKE_TILTAKSDAG,
    IKKE_RETT_TIL_TILTAKSPENGER,
    ;

    fun tilMeldekortDagStatus(): MeldekortDagStatus = when (this) {
        DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
        DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
        FRAVÆR_SYK -> MeldekortDagStatus.FRAVÆR_SYK
        FRAVÆR_SYKT_BARN -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
        FRAVÆR_GODKJENT_AV_NAV -> MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV
        FRAVÆR_ANNET -> MeldekortDagStatus.FRAVÆR_ANNET
        IKKE_BESVART -> MeldekortDagStatus.IKKE_BESVART
        IKKE_TILTAKSDAG -> MeldekortDagStatus.IKKE_TILTAKSDAG
        IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
    }
}

fun MeldekortDagStatus.tilDTO(): MeldekortDagStatusDTO = when (this) {
    MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
    MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatusDTO.DELTATT_MED_LØNN_I_TILTAKET
    MeldekortDagStatus.FRAVÆR_SYK -> MeldekortDagStatusDTO.FRAVÆR_SYK
    MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldekortDagStatusDTO.FRAVÆR_SYKT_BARN
    MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> MeldekortDagStatusDTO.FRAVÆR_GODKJENT_AV_NAV
    MeldekortDagStatus.FRAVÆR_ANNET -> MeldekortDagStatusDTO.FRAVÆR_ANNET
    MeldekortDagStatus.IKKE_BESVART -> MeldekortDagStatusDTO.IKKE_BESVART
    MeldekortDagStatus.IKKE_TILTAKSDAG -> MeldekortDagStatusDTO.IKKE_TILTAKSDAG
    MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
}
