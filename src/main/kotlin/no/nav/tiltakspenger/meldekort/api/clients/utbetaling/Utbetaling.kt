package no.nav.tiltakspenger.meldekort.api.clients.utbetaling

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortBeregning
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import java.time.LocalDate
import java.util.UUID

interface Utbetaling {
    suspend fun sendTilUtbetaling(sakId: String, behandling: MeldekortBeregning): String

    suspend fun hentPeriodisertUtbetalingsgrunnlag(meldekortId: UUID): List<UtbetalingGrunnlagPeriode>
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

fun List<UtbetalingGrunnlagPeriode>.hentGrunnlagForDato(dato: LocalDate): UtbetalingGrunnlag {
    val grunnlag = this.find { it.fom <= dato && it.tom >= dato }
        ?: throw IllegalArgumentException("Fant ingen grunnlag for dato $dato")

    return UtbetalingGrunnlag(grunnlag.antallBarn, grunnlag.sats, grunnlag.satsDelvis, grunnlag.satsBarn, grunnlag.satsBarnDelvis)
}
