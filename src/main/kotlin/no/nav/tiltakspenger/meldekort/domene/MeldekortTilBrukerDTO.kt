package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.meldekort.clients.utils.toNorskUkenummer
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatusDTO.Companion.toDTO
import java.time.Clock
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
    val kanSendes: LocalDateTime?,
)

enum class MeldekortStatusDTO {
    INNSENDT,
    KAN_UTFYLLES,
    IKKE_KLAR,
    DEAKTIVERT,
    ;

    companion object {
        fun MeldekortStatus.toDTO(): MeldekortStatusDTO = when (this) {
            MeldekortStatus.INNSENDT -> INNSENDT
            MeldekortStatus.KAN_UTFYLLES -> KAN_UTFYLLES
            MeldekortStatus.IKKE_KLAR -> IKKE_KLAR
            MeldekortStatus.DEAKTIVERT -> DEAKTIVERT
        }
    }
}

data class MeldekortDagTilBrukerDTO(
    val dag: LocalDate,
    val status: MeldekortDagStatusDTO,
)

fun Meldekort.tilMeldekortTilBrukerDTO(clock: Clock): MeldekortTilBrukerDTO {
    return MeldekortTilBrukerDTO(
        id = id.toString(),
        meldeperiodeId = meldeperiode.id.toString(),
        kjedeId = meldeperiode.kjedeId.toString(),
        versjon = meldeperiode.versjon,
        fraOgMed = periode.fraOgMed,
        tilOgMed = periode.tilOgMed,
        uke1 = periode.fraOgMed.toNorskUkenummer(),
        uke2 = periode.tilOgMed.toNorskUkenummer(),
        status = status(clock).toDTO(),
        maksAntallDager = meldeperiode.maksAntallDagerForPeriode,
        innsendt = mottatt,
        dager = dager.toDto(),
        kanSendes = klarTilInnsendingDateTime(clock),
    )
}

fun List<MeldekortDag>.toDto(): List<MeldekortDagTilBrukerDTO> {
    return this.map { dag ->
        MeldekortDagTilBrukerDTO(
            dag = dag.dag,
            status = dag.status.tilDTO(),
        )
    }
}
