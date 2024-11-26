package no.nav.tiltakspenger.meldekort.routes.meldekort

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.tiltakspenger.meldekort.domene.Meldekort

data class MeldekortDTO(
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

fun Meldekort.toDTO(): MeldekortDTO {
    return MeldekortDTO(
        id = this.id,
        sakId = this.sakId,
        rammevedtakId = this.rammevedtakId,
        fnr = this.fnr,
        forrigeMeldekortId = this.forrigeMeldekortId,
        fraOgMed = this.fraOgMed,
        tilOgMed = this.tilOgMed,
        meldekortDager = this.meldekortDager,
        status = this.status,
        iverksattTidspunkt = this.iverksattTidspunkt
    )
}