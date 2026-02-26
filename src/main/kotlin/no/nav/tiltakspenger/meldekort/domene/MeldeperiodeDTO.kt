package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO

data class MeldeperiodeDTO(
    val meldeperiodeId: String,
    val kjedeId: String,
    val periode: PeriodeDTO,
    val maksAntallDagerForPeriode: Int,
)

fun Meldeperiode.tilMeldeperiodeDTO(): MeldeperiodeDTO {
    return MeldeperiodeDTO(
        meldeperiodeId = id.toString(),
        kjedeId = kjedeId.toString(),
        periode = periode.toDTO(),
        maksAntallDagerForPeriode = maksAntallDagerForPeriode,
    )
}
