package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.meldekort.clients.utils.toNorskUkenummer
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldekortTilBrukerDTO(
    val id: String,
    val meldeperiodeId: String,
    val kjedeId: String,
    val versjon: Int,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val uke1: Int,
    val uke2: Int,
    val status: MeldekortStatusDTO,
    val maksAntallDager: Int,
    val innsendt: LocalDateTime?,
    val dager: List<MeldekortDagTilBrukerDTO>,
    val kanSendes: LocalDate?,
)

enum class MeldekortStatusDTO {
    INNSENDT,
    KAN_UTFYLLES,
    IKKE_KLAR,
    DEAKTIVERT,
}

data class MeldekortDagTilBrukerDTO(
    val dag: LocalDate,
    val status: MeldekortDagStatusDTO,
    val harRett: Boolean,
)

fun Meldekort.tilMeldekortTilBrukerDTO(): MeldekortTilBrukerDTO {
    return MeldekortTilBrukerDTO(
        id = id.toString(),
        meldeperiodeId = meldeperiode.id.toString(),
        kjedeId = meldeperiode.kjedeId.toString(),
        versjon = meldeperiode.versjon,
        fraOgMed = periode.fraOgMed,
        tilOgMed = periode.tilOgMed,
        uke1 = periode.fraOgMed.toNorskUkenummer(),
        uke2 = periode.tilOgMed.toNorskUkenummer(),
        status = when (status) {
            MeldekortStatus.INNSENDT -> MeldekortStatusDTO.INNSENDT
            MeldekortStatus.KAN_UTFYLLES -> MeldekortStatusDTO.KAN_UTFYLLES
            MeldekortStatus.IKKE_KLAR -> MeldekortStatusDTO.IKKE_KLAR
            MeldekortStatus.DEAKTIVERT -> MeldekortStatusDTO.DEAKTIVERT
        },
        maksAntallDager = meldeperiode.maksAntallDagerForPeriode,
        innsendt = mottatt,
        dager = dager.map { dag ->
            MeldekortDagTilBrukerDTO(
                dag = dag.dag,
                harRett = meldeperiode.girRett[dag.dag] == true,
                status = dag.status.tilDTO(),
            )
        },
        kanSendes = kanSendes,
    )
}
