package no.nav.tiltakspenger.meldekort.api.domene

import java.time.LocalDate
import java.util.*

const val antallEgenmeldingsdager = 3
const val antallArbeidsgiverdager = 13
const val dagerKarantene = 16L - 1

data class MeldekortBeregning(
    val utløsendeMeldekortId: UUID,
    val utbetalingDager: List<UtbetalingDag> = mutableListOf(),
    val saksbehandler: String,
) {
    private var sykTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var egenmeldingsdagerSyk: Int = antallEgenmeldingsdager
    private var sykKaranteneDag: LocalDate? = null

    private var syktBarnTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var egenmeldingsdagerSyktBarn: Int = antallEgenmeldingsdager
    private var syktBarnKaranteneDag: LocalDate? = null

    companion object {
        fun beregnUtbetalingsDager(meldekortId: UUID, meldekortDager: List<MeldekortDag>, saksbehandler: String) =
            MeldekortBeregning(utløsendeMeldekortId = meldekortId, saksbehandler = saksbehandler).apply {
                lagUtbetalingsdager(meldekortDager)
            }
    }

    private fun lagUtbetalingsdager(meldekortDager: List<MeldekortDag>) {
        for (meldekortdag in meldekortDager) {
            when (meldekortdag.status) {
                MeldekortDagStatus.IKKE_UTFYLT -> {
                    ikkeUtfylt(meldekortdag)
                }

                MeldekortDagStatus.SPERRET -> sperret(meldekortdag)
                MeldekortDagStatus.DELTATT -> deltatt(meldekortdag)
                MeldekortDagStatus.IKKE_DELTATT -> ikkeDeltatt(meldekortdag)
                MeldekortDagStatus.FRAVÆR_SYK -> fraværSyk(meldekortdag)
                MeldekortDagStatus.FRAVÆR_SYKT_BARN -> fraværSykBarn(meldekortdag)
                MeldekortDagStatus.FRAVÆR_VELFERD -> gyldigFravær(meldekortdag)
                MeldekortDagStatus.LØNN_FOR_TID_I_ARBEID -> gyldigFravær(meldekortdag)
            }
        }
    }

    private fun ikkeUtfylt(dag: MeldekortDag) {
        checkNotNull(dag.tiltak) { "Tiltak må være satt for alle dager" }
        sjekkSykKarantene(dag.dato)
        sjekkSykBarnKarantene(dag.dato)
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
            løpenr = dag.løpenr,
            meldekortId = dag.meldekortId,
        )
    }

    private fun gyldigFravær(dag: MeldekortDag) {
        checkNotNull(dag.tiltak) { "Tiltak må være satt for alle dager" }
        sjekkSykKarantene(dag.dato)
        sjekkSykBarnKarantene(dag.dato)
        leggTilUtbetalingDag(
            dag = dag.dato,
            tiltakType = dag.tiltak.typeKode,
            deltagerStatus = DeltagerStatus.GyldigFravær,
            status = UtbetalingStatus.FullUtbetaling,
            løpenr = dag.løpenr,
            meldekortId = dag.meldekortId,
        )
    }

    private fun sperret(dag: MeldekortDag) {
        sjekkSykKarantene(dag.dato)
        sjekkSykBarnKarantene(dag.dato)
        leggTilUtbetalingDag(
            dag = dag.dato,
            tiltakType = "UTEN_TILTAK",
            deltagerStatus = DeltagerStatus.IkkeDeltatt,
            status = UtbetalingStatus.IngenUtbetaling,
            løpenr = dag.løpenr,
            meldekortId = dag.meldekortId,
        )
    }

    private fun ikkeDeltatt(dag: MeldekortDag) {
        checkNotNull(dag.tiltak) { "Tiltak må være satt for alle dager" }
        sjekkSykKarantene(dag.dato)
        sjekkSykBarnKarantene(dag.dato)
        leggTilUtbetalingDag(
            dag = dag.dato,
            tiltakType = dag.tiltak.typeKode,
            deltagerStatus = DeltagerStatus.IkkeDeltatt,
            status = UtbetalingStatus.IngenUtbetaling,
            løpenr = dag.løpenr,
            meldekortId = dag.meldekortId,
        )
    }

    private fun fraværSyk(dag: MeldekortDag) {
        checkNotNull(dag.tiltak) { "Tiltak må være satt for alle dager" }
        when (sykTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (egenmeldingsdagerSyk > 0) {
                    egenmeldingsdagerSyk--
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.Syk,
                        status = UtbetalingStatus.FullUtbetaling,
                        løpenr = dag.løpenr,
                        meldekortId = dag.meldekortId,
                    )
                } else {
                    egenmeldingsdagerSyk = antallArbeidsgiverdager
                    egenmeldingsdagerSyk--
                    sykTilstand = SykTilstand.DelvisUtbetaling
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.Syk,
                        status = UtbetalingStatus.DelvisUtbetaling,
                        løpenr = dag.løpenr,
                        meldekortId = dag.meldekortId,
                    )
                }
            }

            SykTilstand.DelvisUtbetaling -> {
                if (egenmeldingsdagerSyk > 0) {
                    egenmeldingsdagerSyk--
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.Syk,
                        status = UtbetalingStatus.DelvisUtbetaling,
                        løpenr = dag.løpenr,
                        meldekortId = dag.meldekortId,
                    )
                    if (egenmeldingsdagerSyk == 0) {
                        sykTilstand = SykTilstand.Karantene
                        sykKaranteneDag = dag.dato.plusDays(dagerKarantene)
                    }
                } else {
                    sykTilstand = SykTilstand.Karantene
                    sykKaranteneDag = dag.dato.plusDays(dagerKarantene)
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.Syk,
                        status = UtbetalingStatus.IngenUtbetaling,
                        løpenr = dag.løpenr,
                        meldekortId = dag.meldekortId,
                    )
                }
            }

            SykTilstand.Karantene -> {
                sjekkSykKarantene(dag.dato)
                sjekkSykBarnKarantene(dag.dato)
                sykKaranteneDag = dag.dato.plusDays(dagerKarantene)
                leggTilUtbetalingDag(
                    dag = dag.dato,
                    tiltakType = dag.tiltak.typeKode,
                    deltagerStatus = DeltagerStatus.Syk,
                    status = UtbetalingStatus.IngenUtbetaling,
                    løpenr = dag.løpenr,
                    meldekortId = dag.meldekortId,
                )
            }
        }
    }

    private fun fraværSykBarn(dag: MeldekortDag) {
        checkNotNull(dag.tiltak) { "Tiltak må være satt for alle dager" }
        when (syktBarnTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (egenmeldingsdagerSyktBarn > 0) {
                    egenmeldingsdagerSyktBarn--
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.SyktBarn,
                        status = UtbetalingStatus.FullUtbetaling,
                        løpenr = dag.løpenr,
                        meldekortId = dag.meldekortId,
                    )
                } else {
                    egenmeldingsdagerSyktBarn = antallArbeidsgiverdager
                    egenmeldingsdagerSyktBarn--
                    syktBarnTilstand = SykTilstand.DelvisUtbetaling
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        deltagerStatus = DeltagerStatus.SyktBarn,
                        status = UtbetalingStatus.DelvisUtbetaling,
                        tiltakType = dag.tiltak.typeKode,
                        løpenr = dag.løpenr,
                        meldekortId = dag.meldekortId,
                    )
                }
            }

            SykTilstand.DelvisUtbetaling -> {
                if (egenmeldingsdagerSyktBarn > 0) {
                    egenmeldingsdagerSyktBarn--
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.SyktBarn,
                        status = UtbetalingStatus.DelvisUtbetaling,
                        løpenr = dag.løpenr,
                        meldekortId = dag.meldekortId,
                    )
                    if (egenmeldingsdagerSyktBarn == 0) {
                        syktBarnTilstand = SykTilstand.Karantene
                        syktBarnKaranteneDag = dag.dato.plusDays(dagerKarantene)
                    }
                } else {
                    syktBarnTilstand = SykTilstand.Karantene
                    syktBarnKaranteneDag = dag.dato.plusDays(dagerKarantene)
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.typeKode,
                        deltagerStatus = DeltagerStatus.SyktBarn,
                        status = UtbetalingStatus.IngenUtbetaling,
                        løpenr = dag.løpenr,
                        meldekortId = dag.meldekortId,
                    )
                }
            }

            SykTilstand.Karantene -> {
                sjekkSykKarantene(dag.dato)
                sjekkSykBarnKarantene(dag.dato)
                leggTilUtbetalingDag(
                    dag = dag.dato,
                    tiltakType = dag.tiltak.typeKode,
                    deltagerStatus = DeltagerStatus.SyktBarn,
                    status = UtbetalingStatus.IngenUtbetaling,
                    løpenr = dag.løpenr,
                    meldekortId = dag.meldekortId,
                )
            }
        }
    }

    // alle debug felter som blir satt i denne skal bort. (fra kvote og ned)
    private fun leggTilUtbetalingDag(
        dag: LocalDate,
        deltagerStatus: DeltagerStatus,
        status: UtbetalingStatus,
        tiltakType: String,
        løpenr: Int,
        meldekortId: UUID,
    ) {
        utbetalingDager.addLast(
            UtbetalingDag(
                deltagerStatus = deltagerStatus,
                dag = dag,
                status = status,
                tiltakType = tiltakType,
                løpenr = løpenr,
                meldekortId = meldekortId,
                kvote = egenmeldingsdagerSyk,
                kvoteBarn = egenmeldingsdagerSyktBarn,
                sykKaranteneDag = sykKaranteneDag,
                sykBarnKaranteneDag = syktBarnKaranteneDag,
                tilstandSyk = sykTilstand,
                tilstandSykBarn = syktBarnTilstand,
            ),
        )
    }

    private fun sjekkSykKarantene(dag: LocalDate) {
        if (sykTilstand == SykTilstand.Karantene) {
            if (sykKaranteneDag != null) {
                if (dag.isAfter(sykKaranteneDag)) {
                    sykKaranteneDag = null
                    egenmeldingsdagerSyk = 3
                    sykTilstand = SykTilstand.FullUtbetaling
                }
            }
        }
    }

    private fun sjekkSykBarnKarantene(dag: LocalDate) {
        if (syktBarnTilstand == SykTilstand.Karantene) {
            if (syktBarnKaranteneDag != null) {
                if (dag.isAfter(syktBarnKaranteneDag)) {
                    syktBarnKaranteneDag = null
                    egenmeldingsdagerSyktBarn = 3
                    syktBarnTilstand = SykTilstand.FullUtbetaling
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
