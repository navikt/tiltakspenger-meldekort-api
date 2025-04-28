package no.nav.tiltakspenger.meldekort.domene

/**
 * Denne er reservert til innsending og skal ikke brukes i [Meldekort]
 */
enum class MeldekortDagStatus {
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_VELFERD_GODKJENT_AV_NAV,
    FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV,
    IKKE_REGISTRERT,
}
