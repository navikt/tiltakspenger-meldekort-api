package no.nav.tiltakspenger.meldekort.api.domene

import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import java.time.LocalDate
import java.util.UUID

const val antallEgenmeldingsdager = 3
const val antallArbeidsgiverdager = 13
const val dagerKarantene = 16L - 1

data class MeldekortBeregning(
    val utløsendeMeldekortId: UUID,
    val utbetalingDager: MutableList<UtbetalingDag> = mutableListOf(),
    val saksbehandler: String,
) {
    private var sykTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var egenmeldingsdagerSyk: Int = antallEgenmeldingsdager
    private var sykKaranteneDag: LocalDate? = null
    private var sisteSykedag: LocalDate? = null

    private var syktBarnTilstand: SykTilstand = SykTilstand.FullUtbetaling
    private var egenmeldingsdagerSyktBarn: Int = antallEgenmeldingsdager
    private var syktBarnKaranteneDag: LocalDate? = null
    private var sisteSyktBarnSykedag: LocalDate? = null

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
                MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> deltatt(meldekortdag)
                MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> deltattMedLønn(meldekortdag)
                MeldekortDagStatus.IKKE_DELTATT -> ikkeDeltatt(meldekortdag)
                MeldekortDagStatus.FRAVÆR_SYK -> fraværSyk(meldekortdag)
                MeldekortDagStatus.FRAVÆR_SYKT_BARN -> fraværSykBarn(meldekortdag)
                MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV -> gyldigFravær(meldekortdag)
                MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> ugyldigFravær(meldekortdag)
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
            tiltakType = dag.tiltak.tiltakstype,
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
            tiltakType = dag.tiltak.tiltakstype,
            status = UtbetalingStatus.FullUtbetaling,
            løpenr = dag.løpenr,
            meldekortId = dag.meldekortId,
        )
    }

    private fun ugyldigFravær(dag: MeldekortDag) {
        checkNotNull(dag.tiltak) { "Tiltak må være satt for alle dager" }
        sjekkSykKarantene(dag.dato)
        sjekkSykBarnKarantene(dag.dato)
        leggTilUtbetalingDag(
            dag = dag.dato,
            tiltakType = dag.tiltak.tiltakstype,
            status = UtbetalingStatus.IngenUtbetaling,
            løpenr = dag.løpenr,
            meldekortId = dag.meldekortId,
        )
    }

    private fun sperret(dag: MeldekortDag) {
        sjekkSykKarantene(dag.dato)
        sjekkSykBarnKarantene(dag.dato)
        leggTilUtbetalingDag(
            dag = dag.dato,
            tiltakType = null,
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
            tiltakType = dag.tiltak.tiltakstype,
            status = UtbetalingStatus.IngenUtbetaling,
            løpenr = dag.løpenr,
            meldekortId = dag.meldekortId,
        )
    }

    private fun deltattMedLønn(dag: MeldekortDag) {
        checkNotNull(dag.tiltak) { "Tiltak må være satt for alle dager" }
        sjekkSykKarantene(dag.dato)
        sjekkSykBarnKarantene(dag.dato)
        leggTilUtbetalingDag(
            dag = dag.dato,
            tiltakType = dag.tiltak.tiltakstype,
            status = UtbetalingStatus.IngenUtbetaling,
            løpenr = dag.løpenr,
            meldekortId = dag.meldekortId,
        )
    }

    private fun fraværSyk(dag: MeldekortDag) {
        checkNotNull(dag.tiltak) { "Tiltak må være satt for alle dager" }
        sisteSykedag = dag.dato
        when (sykTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (egenmeldingsdagerSyk > 0) {
                    egenmeldingsdagerSyk--
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.tiltakstype,
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
                        tiltakType = dag.tiltak.tiltakstype,
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
                        tiltakType = dag.tiltak.tiltakstype,
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
                        tiltakType = dag.tiltak.tiltakstype,
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
                    tiltakType = dag.tiltak.tiltakstype,
                    status = UtbetalingStatus.IngenUtbetaling,
                    løpenr = dag.løpenr,
                    meldekortId = dag.meldekortId,
                )
            }
        }
    }

    private fun fraværSykBarn(dag: MeldekortDag) {
        checkNotNull(dag.tiltak) { "Tiltak må være satt for alle dager" }
        sisteSykedag = dag.dato
        when (syktBarnTilstand) {
            SykTilstand.FullUtbetaling -> {
                if (egenmeldingsdagerSyktBarn > 0) {
                    egenmeldingsdagerSyktBarn--
                    leggTilUtbetalingDag(
                        dag = dag.dato,
                        tiltakType = dag.tiltak.tiltakstype,
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
                        status = UtbetalingStatus.DelvisUtbetaling,
                        tiltakType = dag.tiltak.tiltakstype,
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
                        tiltakType = dag.tiltak.tiltakstype,
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
                        tiltakType = dag.tiltak.tiltakstype,
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
                    tiltakType = dag.tiltak.tiltakstype,
                    status = UtbetalingStatus.IngenUtbetaling,
                    løpenr = dag.løpenr,
                    meldekortId = dag.meldekortId,
                )
            }
        }
    }

    private fun leggTilUtbetalingDag(
        dag: LocalDate,
        status: UtbetalingStatus,
        // Settes til null dersom det er sperret (ikke rett på disse dagene)
        tiltakType: TiltakstypeSomGirRett?,
        løpenr: Int,
        meldekortId: UUID,
    ) {
        utbetalingDager.addLast(
            UtbetalingDag(
                dag = dag,
                status = status,
                tiltakType = tiltakType,
                løpenr = løpenr,
                meldekortId = meldekortId,
            ),
        )
    }

    private fun sjekkSykKarantene(dag: LocalDate) {
        if (sisteSykedag != null) {
            if (dag.isAfter(sisteSykedag!!.plusDays(dagerKarantene))) {
                sykKaranteneDag = null
                egenmeldingsdagerSyk = 3
                sykTilstand = SykTilstand.FullUtbetaling
            }
        }
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
        if (sisteSyktBarnSykedag != null) {
            if (dag.isAfter(sisteSyktBarnSykedag!!.plusDays(dagerKarantene))) {
                syktBarnKaranteneDag = null
                egenmeldingsdagerSyktBarn = 3
                syktBarnTilstand = SykTilstand.FullUtbetaling
            }
        }
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
