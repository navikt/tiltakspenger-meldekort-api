package no.nav.tiltakspenger.meldekort.routes.meldekort

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import java.time.LocalDate

/**
 * DTO for [no.nav.tiltakspenger.meldekort.domene.Meldekort]
 */
private data class MeldekortDTO(
    val id: String,
    val fnr: String,
    val periode: PeriodeDTO,
    val meldeperiodeKjedeId: String,
    val dager: List<Dag>,
    val status: String,
) {
    data class Dag(
        val dag: LocalDate,
        val status: String,
    )
}

fun Meldekort.toJson(): String {
    return MeldekortDTO(
        id = this.id.toString(),
        fnr = this.fnr.toString(),
        periode = this.periode.toDTO(),
        meldeperiodeKjedeId = this.meldeperiodeKjedeId.toString(),
        dager = this.dager.map {
            MeldekortDTO.Dag(
                dag = it.dag,
                status = when (it.status) {
                    MeldekortDagStatus.DELTATT -> "DELTATT"
                    MeldekortDagStatus.FRAVÆR_SYK -> "FRAVÆR_SYK"
                    MeldekortDagStatus.FRAVÆR_SYKT_BARN -> "FRAVÆR_SYKT_BARN"
                    MeldekortDagStatus.FRAVÆR_ANNET -> "FRAVÆR_ANNET"
                    MeldekortDagStatus.IKKE_DELTATT -> "IKKE_DELTATT"
                    MeldekortDagStatus.IKKE_REGISTRERT -> "IKKE_REGISTRERT"
                    MeldekortDagStatus.SPERRET -> "SPERRET"
                },
            )
        },
        status = when (this.status) {
            MeldekortStatus.INNSENDT -> "INNSENDT"
            MeldekortStatus.KAN_UTFYLLES -> "KAN_UTFYLLES"
            MeldekortStatus.KAN_IKKE_UTFYLLES -> "KAN_IKKE_UTFYLLES"
        },
    ).let { serialize(it) }
}
