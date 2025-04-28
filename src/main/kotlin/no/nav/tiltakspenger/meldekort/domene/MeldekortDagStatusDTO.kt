package no.nav.tiltakspenger.meldekort.domene

enum class MeldekortDagStatusDTO {
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_VELFERD_GODKJENT_AV_NAV,
    FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV,
    IKKE_REGISTRERT,
    ;

    fun tilMeldekortDagStatus(): MeldekortDagStatus = when (this) {
        DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
        DELTATT_MED_LØNN_I_TILTAKET -> MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET
        FRAVÆR_SYK -> MeldekortDagStatus.FRAVÆR_SYK
        FRAVÆR_SYKT_BARN -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
        FRAVÆR_VELFERD_GODKJENT_AV_NAV -> MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV
        FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
        IKKE_REGISTRERT -> MeldekortDagStatus.IKKE_REGISTRERT
    }
}

fun MeldekortDagStatus.tilDTO(): MeldekortDagStatusDTO = when (this) {
    MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
    MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> throw IllegalStateException("Deltatt med lønn er ikke implementert ennå")
    MeldekortDagStatus.FRAVÆR_SYK -> MeldekortDagStatusDTO.FRAVÆR_SYK
    MeldekortDagStatus.FRAVÆR_SYKT_BARN -> MeldekortDagStatusDTO.FRAVÆR_SYKT_BARN
    MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV -> MeldekortDagStatusDTO.FRAVÆR_VELFERD_GODKJENT_AV_NAV
    MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> MeldekortDagStatusDTO.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
    MeldekortDagStatus.IKKE_REGISTRERT -> MeldekortDagStatusDTO.IKKE_REGISTRERT
}
