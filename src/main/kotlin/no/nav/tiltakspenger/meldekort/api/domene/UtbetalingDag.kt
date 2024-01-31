package no.nav.tiltakspenger.meldekort.api.domene

import java.time.LocalDate

data class UtbetalingDag(
    val deltagerStatus: DeltagerStatus,
    val dag: LocalDate,
    val status: UtbetalingStatus,
    val kvote: Int,
    val kvoteBarn: Int,
    val sykKaranteneDag: LocalDate?,
    val sykBarnKaranteneDag: LocalDate?,
    val tilstandSyk: SykTilstand,
    val tilstandSykBarn: SykTilstand,
) {
    override fun toString(): String {
        return "${deltagerStatus.toString().padEnd(15)} $dag  ${dag.dayOfWeek.toString().padEnd(12)} \t $status \t kvote=$kvote \t kvoteBarn=${kvoteBarn.toString().padEnd(12)} \t karanteneDag=$sykKaranteneDag \t karanteneBarnDag=$sykKaranteneDag \t tilstandSyk=$tilstandSyk \t tilstandSykBarn=$tilstandSykBarn"
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
}
