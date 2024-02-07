package no.nav.tiltakspenger.meldekort.api.domene

import java.time.LocalDate
import java.util.UUID

const val dagerFullYtelse = 3
const val dagerDelvisYtelse = 13
const val dagerKarantene = 16L - 1

data class MeldekortBehandling(
    val id: UUID = UUID.randomUUID(),
    val utbetalingDager: List<UtbetalingDag> = mutableListOf(),
    val saksbehandler: String,
) {
    private var sykTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var kvoteSyk: Int = dagerFullYtelse
    private var sykKaranteneDag: LocalDate? = null

    private var sykBarnTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var kvoteSykBarn: Int = dagerFullYtelse
    private var sykBarnKaranteneDag: LocalDate? = null

    companion object {
        fun lagBehandling(meldekortDager: List<MeldekortDag>, saksbehandler: String) =
            MeldekortBehandling(saksbehandler = saksbehandler).apply {
                lagUtbetalingsdager(meldekortDager)
            }
    }

    private fun lagUtbetalingsdager(meldekortDager: List<MeldekortDag>) {
        for (meldekortdag in meldekortDager) {
            when (meldekortdag.status) {
                MeldekortDagStatus.IKKE_UTFYLT -> {}
                MeldekortDagStatus.DELTATT -> deltatt(meldekortdag)
                MeldekortDagStatus.IKKE_DELTATT -> ikkeDeltatt(meldekortdag)
                MeldekortDagStatus.FRAVÆR_SYK -> fraværSyk(meldekortdag)
                MeldekortDagStatus.FRAVÆR_SYKT_BARN -> fraværSykBarn(meldekortdag)
                MeldekortDagStatus.FRAVÆR_VELFERD -> gyldigFravær(meldekortdag)
                MeldekortDagStatus.LØNN_FOR_TID_I_ARBEID -> gyldigFravær(meldekortdag)
            }
        }
    }

    private fun deltatt(dag: MeldekortDag) {
        checkNotNull(dag.tiltak) { "Tiltak må være satt for alle dager" }
        sjekkSykKarantene(dag.dato)
        sjekkSykBarnKarantene(dag.dato)

        leggTilUtbetalingDag(
            dag = dag.dato,
            tiltakType = dag.tiltak.typeKode,
            deltagerStatus = DeltagerStatus.Deltatt,
            status = UtbetalingStatus.FullUtbetaling,
        )
    }

    private fun gyldigFravær(dag: MeldekortDag) {
        checkNotNull(dag.tiltak) { "Tiltak må være satt for alle dager" }
        leggTilUtbetalingDag(
            dag = dag.dato,
            tiltakType = dag.tiltak.typeKode,
            deltagerStatus = DeltagerStatus.GyldigFravær,
            status = UtbetalingStatus.FullUtbetaling,
        )
    }

    private fun ikkeDeltatt(dag: MeldekortDag) {
        checkNotNull(dag.tiltak) { "Tiltak må være satt for alle dager" }
        leggTilUtbetalingDag(
            dag = dag.dato,
            tiltakType = dag.tiltak.typeKode,
            deltagerStatus = DeltagerStatus.IkkeDeltatt,
            status = UtbetalingStatus.IngenUtbetaling,
        )
    }

    private fun fraværSyk(dag: MeldekortDag) {
        checkNotNull(dag.tiltak) { "Tiltak må være satt for alle dager" }
        when (sykTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (kvoteSyk > 0) {
                    kvoteSyk--
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.Syk,
                        status = UtbetalingStatus.FullUtbetaling,
                    )
                } else {
                    kvoteSyk = dagerDelvisYtelse
                    kvoteSyk--
                    sykTilstand = SykTilstand.DelvisUtbetaling
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.Syk,
                        status = UtbetalingStatus.DelvisUtbetaling,
                    )
                }
            }

            SykTilstand.DelvisUtbetaling -> {
                if (kvoteSyk > 0) {
                    kvoteSyk--
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.Syk,
                        status = UtbetalingStatus.DelvisUtbetaling,
                    )
                } else {
                    sykTilstand = SykTilstand.Karantene
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.Syk,
                        status = UtbetalingStatus.IngenUtbetaling,
                    )
                }
            }

            SykTilstand.Karantene -> {
                if (sykKaranteneDag != null) {
                    sykKaranteneDag = dag.dato.plusDays(dagerKarantene)
                }
                leggTilUtbetalingDag(
                    dag = dag.dato,
                    tiltakType = dag.tiltak.typeKode,
                    deltagerStatus = DeltagerStatus.Syk,
                    status = UtbetalingStatus.IngenUtbetaling,
                )
            }
        }
    }

    private fun fraværSykBarn(dag: MeldekortDag) {
        checkNotNull(dag.tiltak) { "Tiltak må være satt for alle dager" }
        when (sykBarnTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (kvoteSykBarn > 0) {
                    kvoteSykBarn--
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.SyktBarn,
                        status = UtbetalingStatus.FullUtbetaling,
                    )
                } else {
                    kvoteSykBarn = dagerDelvisYtelse
                    kvoteSykBarn--
                    sykBarnTilstand = SykTilstand.DelvisUtbetaling
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.SyktBarn,
                        status = UtbetalingStatus.DelvisUtbetaling,
                    )
                }
            }

            SykTilstand.DelvisUtbetaling -> {
                if (kvoteSykBarn > 0) {
                    kvoteSykBarn--
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.SyktBarn,
                        status = UtbetalingStatus.DelvisUtbetaling,
                    )
                } else {
                    sykBarnTilstand = SykTilstand.Karantene
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.SyktBarn,
                        status = UtbetalingStatus.IngenUtbetaling,
                    )
                }
            }

            SykTilstand.Karantene -> {
                if (sykBarnKaranteneDag != null) {
                    sykBarnKaranteneDag = dag.dato.plusDays(dagerKarantene)
                }
                leggTilUtbetalingDag(
                    dag = dag.dato,
                    tiltakType = dag.tiltak.typeKode,
                    deltagerStatus = DeltagerStatus.SyktBarn,
                    status = UtbetalingStatus.IngenUtbetaling,
                )
            }
        }
    }

    private fun leggTilUtbetalingDag(
        dag: LocalDate,
        deltagerStatus: DeltagerStatus,
        status: UtbetalingStatus,
        tiltakType: String,
    ) {
        utbetalingDager.addLast(
            UtbetalingDag(
                deltagerStatus = deltagerStatus,
                dag = dag,
                status = status,
                tiltakType = tiltakType,
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
}

private fun List<Meldekort>.tilDager(): List<MeldekortDag> {
    return this.flatMap { it.meldekortDager }
}

enum class SykTilstand {
    FullUtbetaling,
    DelvisUtbetaling,
    Karantene,
}
