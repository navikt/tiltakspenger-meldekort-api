package no.nav.tiltakspenger.meldekort.domene

/**
 * Denne er reservert til innsending og skal ikke brukes i [Meldekort]
 */
enum class MeldekortDagStatus {
    DELTATT,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_ANNET,
    IKKE_DELTATT,
    IKKE_REGISTRERT,
}
