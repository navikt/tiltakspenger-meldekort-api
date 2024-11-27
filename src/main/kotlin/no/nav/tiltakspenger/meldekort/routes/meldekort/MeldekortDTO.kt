package no.nav.tiltakspenger.meldekort.routes.meldekort

import no.nav.tiltakspenger.meldekort.domene.Meldekort
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldekortDTO(
    val id: String,
    val sakId: String,
    val fnr: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val meldeperiodeId: String,
    val meldekortDager: List<MeldekortDagDTO>,
    val status: String,
    val iverksattTidspunkt: LocalDateTime?,
)

fun Meldekort.toDTO(): MeldekortDTO {
    return MeldekortDTO(
        id = this.id.toString(),
        sakId = this.sakId.toString(),
        fnr = this.fnr.verdi,
        fraOgMed = this.fraOgMed,
        tilOgMed = this.tilOgMed,
        meldeperiodeId = this.meldeperiodeId.verdi,
        meldekortDager = this.meldekortDager.toDTO(),
        status = this.status,
        iverksattTidspunkt = this.iverksattTidspunkt,
    )
}
