package no.nav.tiltakspenger.meldekort.api.domene

import java.time.LocalDate

data class Utfallsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallBarn: Int,
    val utfall: UtfallForPeriode,
)

enum class UtfallForPeriode {
    GIR_RETT_TILTAKSPENGER,
    GIR_IKKE_RETT_TILTAKSPENGER,
    KREVER_MANUELL_VURDERING,
}
