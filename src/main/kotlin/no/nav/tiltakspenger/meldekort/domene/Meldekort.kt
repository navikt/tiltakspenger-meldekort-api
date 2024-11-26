package no.nav.tiltakspenger.meldekort.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class Meldekort(
    val id: String, // TODO Kew: Lag en type for MeldekortId
    val sakId: String,
    val rammevedtakId: String,
    val fnr: String,
    val forrigeMeldekortId: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val meldekortDager: String,
    val status: String,
    val iverksattTidspunkt: LocalDateTime,
)