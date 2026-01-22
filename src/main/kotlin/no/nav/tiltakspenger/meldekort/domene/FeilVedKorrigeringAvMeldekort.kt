package no.nav.tiltakspenger.meldekort.domene

sealed interface FeilVedKorrigeringAvMeldekort {
    data object IkkeSisteMeldekortIKjeden : FeilVedKorrigeringAvMeldekort
    data object IngenEndringer : FeilVedKorrigeringAvMeldekort
}
