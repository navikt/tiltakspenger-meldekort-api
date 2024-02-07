package no.nav.tiltakspenger.meldekort.api.domene

import java.time.LocalDate

data class UtbetalingDag(
    val deltagerStatus: DeltagerStatus,
    val dag: LocalDate,
    val status: UtbetalingStatus,
    val tiltakType: String,
    val kvote: Int,
    val kvoteBarn: Int,
    val sykKaranteneDag: LocalDate?,
    val sykBarnKaranteneDag: LocalDate?,
    val tilstandSyk: SykTilstand,
    val tilstandSykBarn: SykTilstand,
) {
    override fun toString(): String {
        return "${deltagerStatus.toString().padEnd(13)} $dag  ${dag.dayOfWeek.toString().padEnd(10)} ${status.toString().padEnd(18)} kvote=$kvote \t kvoteBarn=${kvoteBarn.toString().padEnd(4)} karanteneDag=${sykKaranteneDag.toString().padEnd(12)} karanteneBarnDag=${sykBarnKaranteneDag.toString().padEnd(12)} tilstandSyk=${tilstandSyk.toString().padEnd(18)} tilstandSykBarn=$tilstandSykBarn"
    }
}

enum class UtbetalingStatus {
    IngenUtbetaling,
    FullUtbetaling,
    DelvisUtbetaling,
}

enum class DeltagerStatus {
    Deltatt,
    IkkeDeltatt,
    Syk,
    SyktBarn,
    GyldigFravær,
}
