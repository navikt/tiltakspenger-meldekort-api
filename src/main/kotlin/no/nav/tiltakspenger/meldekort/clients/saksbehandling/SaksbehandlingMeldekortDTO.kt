package no.nav.tiltakspenger.meldekort.clients.saksbehandling

import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import java.time.LocalDate

data class SaksbehandlingMeldekortDTO(
    val id: String,
    val fnr: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val meldeperiodeId: MeldeperiodeId,
    val meldekortDager: List<SaksbehandlingMeldekortDagDTO>,
    val status: MeldekortStatusDTO,
)

fun Meldekort.toSaksbehandlingMeldekortDTO(): SaksbehandlingMeldekortDTO =
    SaksbehandlingMeldekortDTO(
        id = this.id.toString(),
        fnr = this.fnr.verdi,
        fraOgMed = this.periode.fraOgMed,
        tilOgMed = this.periode.tilOgMed,
        meldeperiodeId = this.meldeperiodeKjedeId,
        meldekortDager = this.dager.toSaksbehandlingDTO(),
        status = this.status.toDTO(),
    )

data class SaksbehandlingMeldekortDagDTO(
    val dag: LocalDate,
    val status: String,
)

fun List<MeldekortDag>.toSaksbehandlingDTO(): List<SaksbehandlingMeldekortDagDTO> =
    this.map { dag ->
        SaksbehandlingMeldekortDagDTO(dag = dag.dag, status = dag.status.name)
    }
