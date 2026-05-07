package no.nav.tiltakspenger.meldekort.meldekort

sealed interface FeilVedKorrigeringAvMeldekort {
    data object IkkeSisteMeldekortIKjeden : FeilVedKorrigeringAvMeldekort
    data object IngenEndringer : FeilVedKorrigeringAvMeldekort
}
