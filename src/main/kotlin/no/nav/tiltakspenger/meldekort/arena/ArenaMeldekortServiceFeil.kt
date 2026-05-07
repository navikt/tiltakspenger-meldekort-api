package no.nav.tiltakspenger.meldekort.arena

sealed interface ArenaMeldekortServiceFeil {
    data class HttpFeil(val status: Int) : ArenaMeldekortServiceFeil
    data object UkjentFeil : ArenaMeldekortServiceFeil
}
