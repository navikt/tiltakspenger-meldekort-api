package no.nav.tiltakspenger.meldekort.api.domene

import java.time.LocalDate

const val dagerFullYtelse = 3
const val dagerDelvisYtelse = 13
const val dagerKarantene = 16L - 1

data class MeldekortBehandling(
    private val meldekort: List<Meldekort>,
    private var utbetalingDager: List<UtbetalingDag> = mutableListOf(),
) {
    private var sykTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var kvoteSyk: Int = dagerFullYtelse
    private var sykKaranteneDag: LocalDate? = null

    private var sykBarnTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var kvoteSykBarn: Int = dagerFullYtelse
    private var sykBarnKaranteneDag: LocalDate? = null

    fun lagUtbetalingsdager(): List<UtbetalingDag> {
        val meldekortdager = meldekort.tilDager()
        initTellere()

        for (meldekortdag in meldekortdager) {
            when (meldekortdag.status) {
                MeldekortDagStatus.IKKE_UTFYLT -> {}
                MeldekortDagStatus.DELTATT -> deltatt(meldekortdag.dato)
                MeldekortDagStatus.IKKE_DELTATT -> ikkeDeltatt(meldekortdag.dato)
                MeldekortDagStatus.FRAVÆR_SYK -> fraværSyk(meldekortdag.dato)
                MeldekortDagStatus.FRAVÆR_SYKT_BARN -> fraværSykBarn(meldekortdag.dato)
                MeldekortDagStatus.FRAVÆR_VELFERD -> gyldigFravær(meldekortdag.dato)
                MeldekortDagStatus.LØNN_FOR_TID_I_ARBEID -> gyldigFravær(meldekortdag.dato)
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

    private fun gyldigFravær(dag: LocalDate) {
        leggTilUtbetalingDag(
            dag = dag,
            deltagerStatus = DeltagerStatus.GyldigFravær,
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
                    kvoteSyk = dagerDelvisYtelse
                    kvoteSyk--
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
                    sykTilstand = SykTilstand.Karantene
                    leggTilUtbetalingDag(
                        dag = dag,
                        deltagerStatus = DeltagerStatus.Syk,
                        status = UtbetalingStatus.IngenUtbetaling,
                    )
                }
            }
            SykTilstand.Karantene -> {
                if (sykKaranteneDag != null) {
                    sykKaranteneDag = dag.plusDays(dagerKarantene)
                }
                leggTilUtbetalingDag(
                    dag = dag,
                    deltagerStatus = DeltagerStatus.Syk,
                    status = UtbetalingStatus.IngenUtbetaling,
                )
            }
        }
    }

    private fun fraværSykBarn(dag: LocalDate) {
        when (sykBarnTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (kvoteSykBarn > 0) {
                    kvoteSykBarn--
                    leggTilUtbetalingDag(
                        dag = dag,
                        deltagerStatus = DeltagerStatus.SyktBarn,
                        status = UtbetalingStatus.FullUtbetaling,
                    )
                } else {
                    kvoteSykBarn = dagerDelvisYtelse
                    kvoteSykBarn--
                    sykBarnTilstand = SykTilstand.DelvisUtbetaling
                    leggTilUtbetalingDag(
                        dag = dag,
                        deltagerStatus = DeltagerStatus.SyktBarn,
                        status = UtbetalingStatus.DelvisUtbetaling,
                    )
                }
            }
            SykTilstand.DelvisUtbetaling -> {
                if (kvoteSykBarn > 0) {
                    kvoteSykBarn--
                    leggTilUtbetalingDag(
                        dag = dag,
                        deltagerStatus = DeltagerStatus.SyktBarn,
                        status = UtbetalingStatus.DelvisUtbetaling,
                    )
                } else {
                    sykBarnTilstand = SykTilstand.Karantene
                    leggTilUtbetalingDag(
                        dag = dag,
                        deltagerStatus = DeltagerStatus.SyktBarn,
                        status = UtbetalingStatus.IngenUtbetaling,
                    )
                }
            }
            SykTilstand.Karantene -> {
                if (sykBarnKaranteneDag != null) {
                    sykBarnKaranteneDag = dag.plusDays(dagerKarantene)
                }
                leggTilUtbetalingDag(
                    dag = dag,
                    deltagerStatus = DeltagerStatus.SyktBarn,
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
                sykKaranteneDag = dag.plusDays(dagerKarantene)
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
                sykBarnKaranteneDag = dag.plusDays(dagerKarantene)
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
