package no.nav.tiltakspenger.meldekort.clients.saksbehandling

import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import java.time.LocalDate
import java.time.LocalDateTime

data class SaksbehandlingMeldekortDTO(
    val id: String,
    val sakId: String,
    val fnr: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val meldeperiodeId: MeldeperiodeId,
    val meldekortDager: List<SaksbehandlingMeldekortDagDTO>,
    val status: String,
    val iverksattTidspunkt: LocalDateTime? = null,
)

fun Meldekort.toSaksbehandlingMeldekortDTO(): SaksbehandlingMeldekortDTO =
    SaksbehandlingMeldekortDTO(
        id = this.id.toString(),
        sakId = this.sakId.toString(),
        fnr = this.fnr.verdi,
        fraOgMed = this.fraOgMed,
        tilOgMed = this.tilOgMed,
        meldeperiodeId = this.meldeperiodeId,
        meldekortDager = this.meldekortDager.toSaksbehandlingDTO(),
        status = this.status,
        iverksattTidspunkt = this.iverksattTidspunkt,
    )

data class SaksbehandlingMeldekortDagDTO(
    val dag: LocalDate,
    val status: String,
)

fun List<MeldekortDag>.toSaksbehandlingDTO(): List<SaksbehandlingMeldekortDagDTO> =
    this.map { dag ->
        SaksbehandlingMeldekortDagDTO(dag = dag.dag, status = dag.status)
    }
