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
    SPERRET,
}

fun toMeldekortDagStatus(status: String): MeldekortDagStatus {
    return when (status) {
        "DELTATT" -> MeldekortDagStatus.DELTATT
        "FRAVÆR_SYK" -> MeldekortDagStatus.FRAVÆR_SYK
        "FRAVÆR_SYKT_BARN" -> MeldekortDagStatus.FRAVÆR_SYKT_BARN
        "FRAVÆR_ANNET" -> MeldekortDagStatus.FRAVÆR_ANNET
        "IKKE_DELTATT" -> MeldekortDagStatus.IKKE_DELTATT
        "IKKE_REGISTRERT" -> MeldekortDagStatus.IKKE_REGISTRERT
        "SPERRET" -> MeldekortDagStatus.SPERRET
        else -> {
            throw IllegalArgumentException("Ingen status for $status")
        }
    }
}
