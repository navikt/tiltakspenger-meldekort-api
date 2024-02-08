package no.nav.tiltakspenger.meldekort.api.domene

import java.time.LocalDate

data class UtbetalingDag(
    val dag: LocalDate,
    val status: UtbetalingStatus,
    val løpenr: Int,
    val tiltakType: String,

    // feltene under her er for debug og skal fjernes
    val deltagerStatus: DeltagerStatus,
    val kvote: Int,
    val kvoteBarn: Int,
    val sykKaranteneDag: LocalDate?,
    val sykBarnKaranteneDag: LocalDate?,
    val tilstandSyk: SykTilstand,
    val tilstandSykBarn: SykTilstand,
) {
    // denne toString er kun for prettyprint i test og skal bort
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
