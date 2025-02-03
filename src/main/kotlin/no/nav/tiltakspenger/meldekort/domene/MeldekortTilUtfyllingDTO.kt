package no.nav.tiltakspenger.meldekort.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class MeldekortTilUtfyllingDTO(
    val id: String,
    val meldeperiodeId: String,
    val meldeperiodeKjedeId: String,
    val versjon: Int,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val status: BrukersMeldekortStatus,
    val innsendt: LocalDateTime?,
    val dager: List<MeldekortDagTilUtfylling>,
)

data class MeldekortDagTilUtfylling(
    override val dag: LocalDate,
    override val status: MeldekortDagStatus,
    val harRett: Boolean,
) : MeldekortDag

fun BrukersMeldekort.tilUtfyllingDTO(): MeldekortTilUtfyllingDTO {
    return MeldekortTilUtfyllingDTO(
        id = this.id.toString(),
        meldeperiodeId = this.meldeperiode.id.toString(),
        meldeperiodeKjedeId = this.meldeperiode.kjedeId.toString(),
        versjon = this.meldeperiode.versjon,
        fraOgMed = this.periode.fraOgMed,
        tilOgMed = this.periode.tilOgMed,
        status = this.status,
        innsendt = this.mottatt,
        dager = this.dager.map { dag ->
            MeldekortDagTilUtfylling(
                dag = dag.dag,
                harRett = dag.harRett,
                status = dag.status,
            )
        },
    )
}
