package no.nav.tiltakspenger.meldekort.domene

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Meldekort(
    // TODO Kew: Lag en type for MeldekortId
    val id: String,
    val sakId: String,
    val rammevedtakId: String,
    val fnr: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val meldekortDager: List<MeldekortDag>,
    val status: String,
    val forrigeMeldekortId: String? = null,
    val iverksattTidspunkt: LocalDateTime? = null,
)

fun genererDummyMeldekort(fnr: String): Meldekort {
    return Meldekort(
        id = UUID.randomUUID().toString(),
        sakId = "asdf",
        rammevedtakId = "asdf",
        fnr = fnr,
        fraOgMed = LocalDate.now(),
        tilOgMed = LocalDate.now(),
        meldekortDager = listOf(),
        status = "asdf",
    )
}
