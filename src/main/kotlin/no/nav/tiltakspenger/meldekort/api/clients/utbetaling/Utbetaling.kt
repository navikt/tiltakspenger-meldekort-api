package no.nav.tiltakspenger.meldekort.api.clients.utbetaling

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortBeregning
import java.time.LocalDate

interface Utbetaling {
    suspend fun sendTilUtbetaling(sakId: String, behandling: MeldekortBeregning): String

    suspend fun hentPeriodisertUtbetalingsgrunnlag(behandlingId: String, fom: LocalDate, tom: LocalDate): List<UtbetalingGrunnlagPeriode>
}

data class UtbetalingGrunnlagPeriode(
    val antallBarn: Int,
    val sats: Int,
    val satsDelvis: Int,
    val satsBarn: Int,
    val satsBarnDelvis: Int,
    val fom: LocalDate,
    val tom: LocalDate,
)

data class UtbetalingGrunnlag(
    val antallBarn: Int,
    val sats: Int,
    val satsDelvis: Int,
    val satsBarn: Int,
    val satsBarnDelvis: Int,
)

data class GrunnlagDTO(
    val behandlingId: String,
    val fom: LocalDate,
    val tom: LocalDate,
)

fun List<UtbetalingGrunnlagPeriode>.hentGrunnlagForDato(dato: LocalDate): UtbetalingGrunnlag {
    val grunnlag = this.find { it.fom <= dato && it.tom >= dato }
        ?: throw IllegalArgumentException("Fant ingen grunnlag for dato $dato")

    return UtbetalingGrunnlag(grunnlag.antallBarn, grunnlag.sats, grunnlag.satsDelvis, grunnlag.satsBarn, grunnlag.satsBarnDelvis)
}
