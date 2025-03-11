package no.nav.tiltakspenger.meldekort.domene

import java.time.LocalDate
import java.time.LocalDateTime

data class MeldekortTilBrukerDTO(
    val id: String,
    val meldeperiodeId: String,
    val kjedeId: String,
    val versjon: Int,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val status: MeldekortStatus,
    val maksAntallDager: Int,
    val innsendt: LocalDateTime?,
    val dager: List<MeldekortDagTilBruker>,
    val kanSendes: Boolean,
)

data class MeldekortDagTilBruker(
    override val dag: LocalDate,
    override val status: MeldekortDagStatus,
    val harRett: Boolean,
) : IMeldekortDag

fun Meldekort.tilBrukerDTO(): MeldekortTilBrukerDTO {
    return MeldekortTilBrukerDTO(
        id = id.toString(),
        meldeperiodeId = meldeperiode.id.toString(),
        kjedeId = meldeperiode.kjedeId.toString(),
        versjon = meldeperiode.versjon,
        fraOgMed = periode.fraOgMed,
        tilOgMed = periode.tilOgMed,
        status = status,
        maksAntallDager = meldeperiode.maksAntallDagerForPeriode,
        innsendt = mottatt,
        dager = dager.map { dag ->
            MeldekortDagTilBruker(
                dag = dag.dag,
                harRett = meldeperiode.girRett[dag.dag] == true,
                status = dag.status,
            )
        },
        kanSendes = this.kanSendes(),
    )
}
