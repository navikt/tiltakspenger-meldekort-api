package no.nav.tiltakspenger.meldekort.api.routes.dto

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
