package no.nav.tiltakspenger.meldekort.api.domene

import java.time.LocalDate

data class MeldekortBehandling(
    private val meldekort: List<Meldekort>,
    private var utbetalingDager: List<UtbetalingDag> = mutableListOf(),
) {
    private var sykTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var kvoteSyk: Int = 3
    private var sykKaranteneDag: LocalDate? = null

    private var sykBarnTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var kvoteSykBarn: Int = 3
    private var sykBarnKaranteneDag: LocalDate? = null

    fun lagUtbetalingsdager(): List<UtbetalingDag> {
        val meldekortdager = meldekort.tilDager()
        initTellere()

        for (meldekortdag in meldekortdager) {
            when (meldekortdag.status) {
                MeldekortDagStatus.IKKE_UTFYLT -> TODO()
                MeldekortDagStatus.DELTATT -> deltatt(meldekortdag.dato)
                MeldekortDagStatus.IKKE_DELTATT -> ikkeDeltatt(meldekortdag.dato)
                MeldekortDagStatus.FRAVÆR_SYK -> fraværSyk(meldekortdag.dato)
                MeldekortDagStatus.FRAVÆR_SYKT_BARN -> TODO()
                MeldekortDagStatus.FRAVÆR_VELFERD -> TODO()
                MeldekortDagStatus.LØNN_FOR_TID_I_ARBEID -> TODO()
            }
        }
        return utbetalingDager
    }

    private fun deltatt(dag: LocalDate) {
        sjekkSykKarantene(dag)
        sjekkSykBarnKarantene(dag)

        leggTilUtbetalingDag(
            dag = dag,
            deltagerStatus = DeltagerStatus.Deltatt,
            status = UtbetalingStatus.FullUtbetaling,
        )
    }

    private fun ikkeDeltatt(dag: LocalDate) {
        leggTilUtbetalingDag(
            dag = dag,
            deltagerStatus = DeltagerStatus.IkkeDeltatt,
            status = UtbetalingStatus.IngenUtbetaling,
        )
    }

    private fun fraværSyk(dag: LocalDate) {
        when (sykTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (kvoteSyk > 0) {
                    kvoteSyk--
                    leggTilUtbetalingDag(
                        dag = dag,
                        deltagerStatus = DeltagerStatus.Syk,
                        status = UtbetalingStatus.FullUtbetaling,
                    )
                } else {
                    kvoteSyk = 13
                    sykTilstand = SykTilstand.DelvisUtbetaling
                    leggTilUtbetalingDag(
                        dag = dag,
                        deltagerStatus = DeltagerStatus.Syk,
                        status = UtbetalingStatus.DelvisUtbetaling,
                    )
                }
            }
            SykTilstand.DelvisUtbetaling -> {
                if (kvoteSyk > 0) {
                    kvoteSyk--
                    leggTilUtbetalingDag(
                        dag = dag,
                        deltagerStatus = DeltagerStatus.Syk,
                        status = UtbetalingStatus.DelvisUtbetaling,
                    )
                } else {
                    kvoteSyk = 13
                    sykTilstand = SykTilstand.Karantene
                    leggTilUtbetalingDag(
                        dag = dag,
                        deltagerStatus = DeltagerStatus.Syk,
                        status = UtbetalingStatus.IngenUtbetaling,
                    )
                }
            }
            SykTilstand.Karantene -> {
                leggTilUtbetalingDag(
                    dag = dag,
                    deltagerStatus = DeltagerStatus.Syk,
                    status = UtbetalingStatus.IngenUtbetaling,
                )
            }
        }
    }

    private fun leggTilUtbetalingDag(dag: LocalDate, deltagerStatus: DeltagerStatus, status: UtbetalingStatus) {
        utbetalingDager.addLast(
            UtbetalingDag(
                deltagerStatus = deltagerStatus,
                dag = dag,
                status = status,
                kvote = kvoteSyk,
                kvoteBarn = kvoteSykBarn,
                sykKaranteneDag = sykKaranteneDag,
                sykBarnKaranteneDag = sykBarnKaranteneDag,
                tilstandSyk = sykTilstand,
                tilstandSykBarn = sykBarnTilstand,
            ),
        )
    }

    private fun sjekkSykKarantene(dag: LocalDate) {
        if (sykTilstand == SykTilstand.Karantene) {
            if (sykKaranteneDag == null) {
                sykKaranteneDag = dag.plusDays(15)
            } else {
                if (dag.isAfter(sykKaranteneDag)) {
                    sykKaranteneDag = null
                    kvoteSyk = 3
                    sykTilstand = SykTilstand.FullUtbetaling
                }
            }
        }
    }

    private fun sjekkSykBarnKarantene(dag: LocalDate) {
        if (sykBarnTilstand == SykTilstand.Karantene) {
            if (sykBarnKaranteneDag == null) {
                sykBarnKaranteneDag = dag.plusDays(15)
            } else {
                if (dag.isAfter(sykBarnKaranteneDag)) {
                    sykBarnKaranteneDag = null
                    kvoteSykBarn = 3
                    sykBarnTilstand = SykTilstand.FullUtbetaling
                }
            }
        }
    }

    private fun initTellere() {
        sykTilstand = SykTilstand.FullUtbetaling
        kvoteSyk = 3
        sykBarnTilstand = SykTilstand.FullUtbetaling
        kvoteSykBarn = 3
        sykKaranteneDag = null
        sykBarnKaranteneDag = null
        utbetalingDager = mutableListOf()
    }
}

private fun List<Meldekort>.tilDager(): List<MeldekortDag> {
    return this.flatMap { it.meldekortDager }
}

enum class SykTilstand {
    FullUtbetaling,
    DelvisUtbetaling,
    Karantene,
}
