package no.nav.tiltakspenger.meldekort.api.service

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortBeregning
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.FRAVÆR_SYK
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.FRAVÆR_SYKT_BARN
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.IKKE_DELTATT
import no.nav.tiltakspenger.meldekort.api.domene.Tiltak
import no.nav.tiltakspenger.meldekort.api.domene.UtbetalingStatus
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

// TODO Kew: Her er det lagt til flere statuser som også må testes!
internal class MeldekortBeregningTest {

    @Test
    fun `manuell test av meldekortberegning`() {
        val meldekortId = UUID.randomUUID()
        val meldekortBeregning =
            MeldekortBeregning.beregnUtbetalingsDager(
                meldekortDager = listOf(
                    MeldekortDag(dato = 29.januar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 30.januar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 31.januar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 1.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 2.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 3.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 4.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 5.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 6.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 7.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 8.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 9.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 10.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 11.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 12.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 13.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 14.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 15.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 16.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 17.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 18.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 19.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 20.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 21.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 22.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 23.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 24.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 25.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 26.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 27.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 28.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 29.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 1.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 2.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 3.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 4.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 5.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 6.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 7.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 8.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 9.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 10.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 11.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 12.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 13.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 14.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 15.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 16.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 17.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 18.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 19.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 20.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 21.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 22.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 23.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 24.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 25.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 26.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 27.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 28.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 29.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 30.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 31.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 1.april(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 2.april(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 3.april(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 4.april(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 5.april(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 6.april(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 7.april(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                ),
                saksbehandler = "saksbehandler",
                meldekortId = UUID.randomUUID(),
            )

        for (dag in meldekortBeregning.utbetalingDager) {
            println(dag)
        }
    }

    @Test
    fun `sjekk at karantene fungerer med nøyaktig 16 dager fravær`() {
        val meldekortId = UUID.randomUUID()
        val meldekortBeregning =
            MeldekortBeregning.beregnUtbetalingsDager(
                meldekortDager = listOf(
                    MeldekortDag(dato = 29.januar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 30.januar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 31.januar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 1.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 2.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 3.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 4.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 5.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 6.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 7.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 8.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 9.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 10.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 11.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 12.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 13.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 14.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 15.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 16.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),

                    MeldekortDag(dato = 17.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 18.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 19.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 20.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 21.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 22.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 23.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 24.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 25.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 26.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 27.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 28.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 29.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 1.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 2.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 3.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 4.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 5.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 6.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 7.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 8.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 9.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 10.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 11.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 12.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 13.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 14.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 15.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 16.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 17.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 18.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 19.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 20.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 21.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 22.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 23.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 24.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 25.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 26.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 27.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 28.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 29.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 30.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 31.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 1.april(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 2.april(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 3.april(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 4.april(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 5.april(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 6.april(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 7.april(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                ),
                saksbehandler = "saksbehandler",
                meldekortId = UUID.randomUUID(),
            )
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[0].status shouldBe UtbetalingStatus.FullUtbetaling
        // fravær syk første dag
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[1].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[2].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[3].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[4].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[5].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[6].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[7].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[8].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[9].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[10].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[11].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[12].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[13].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[14].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[15].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[16].status shouldBe UtbetalingStatus.DelvisUtbetaling
        // vært syk 16 dager karantene starter her
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[17].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[18].status shouldBe UtbetalingStatus.IngenUtbetaling
        // vært syk 18 dager, karantene har blitt flyttet til å telle herifra
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[19].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[20].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[21].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[22].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[23].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[24].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[25].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[26].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[27].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[28].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[29].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[30].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[31].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[32].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[33].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[34].status shouldBe UtbetalingStatus.IngenUtbetaling
        // ferdig med karantene selv om man ikke har deltatt på tiltak
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[35].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[36].status shouldBe UtbetalingStatus.FullUtbetaling
    }

    @Test
    fun `sjekk at karantene telles fra siste fravær, man trenger ikke delta før vi sjekker karantenen `() {
        val meldekortId = UUID.randomUUID()
        val meldekortBeregning =
            MeldekortBeregning.beregnUtbetalingsDager(
                meldekortDager = listOf(
                    MeldekortDag(dato = 1.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 2.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 3.oktober(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 4.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 5.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 6.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 7.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 8.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 9.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 10.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 11.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 12.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 13.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 14.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),

                    MeldekortDag(dato = 15.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 16.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 17.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 18.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 19.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 20.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 21.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 22.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 23.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 24.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 25.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 26.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 27.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 28.oktober(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 29.oktober(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 30.oktober(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 31.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 1.november(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 2.november(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 3.november(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 4.november(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 5.november(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 6.november(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 7.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 8.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 9.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 10.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 11.november(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 12.november(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 13.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 14.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 15.november(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 16.november(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 17.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 18.november(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 19.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 20.november(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 21.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 22.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 23.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 24.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 25.november(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),

                    MeldekortDag(dato = 26.november(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 27.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 28.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 29.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 30.november(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 1.desember(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 2.desember(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 3.desember(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                ),
                saksbehandler = "saksbehandler",
                meldekortId = UUID.randomUUID(),
            )

        for (dag in meldekortBeregning.utbetalingDager) {
            println(dag)
        }

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[0].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[1].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[2].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[3].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[4].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[5].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[6].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[7].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[8].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[9].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[10].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[11].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[12].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[13].status shouldBe UtbetalingStatus.DelvisUtbetaling

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[14].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[15].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[16].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[17].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[18].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[19].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[20].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[21].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[22].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[23].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[24].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[25].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[26].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[27].status shouldBe UtbetalingStatus.IngenUtbetaling

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[28].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[29].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[30].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[31].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[32].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[33].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[34].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[35].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[36].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[37].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[38].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[39].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[40].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[41].status shouldBe UtbetalingStatus.IngenUtbetaling

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[42].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[43].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[44].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[45].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[46].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[47].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[48].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[49].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[50].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[51].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[52].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[53].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[54].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[55].status shouldBe UtbetalingStatus.FullUtbetaling

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[56].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[57].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[58].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[59].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[60].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[61].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[62].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[63].status shouldBe UtbetalingStatus.IngenUtbetaling
    }

    @Test
    fun `fravær med 15 dager i mellom gir delvisUtbetaling`() {
        val meldekortId = UUID.randomUUID()
        val meldekortBeregning =
            MeldekortBeregning.beregnUtbetalingsDager(
                meldekortDager = listOf(
                    MeldekortDag(dato = 1.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 2.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 3.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 4.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 5.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 6.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 7.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 8.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 9.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 10.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 11.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 12.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 13.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 14.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),

                    MeldekortDag(dato = 15.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 16.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 17.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 18.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 19.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 20.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 21.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 22.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 23.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 24.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 25.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 26.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 27.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 28.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                ),
                saksbehandler = "saksbehandler",
                meldekortId = UUID.randomUUID(),
            )

        for (dag in meldekortBeregning.utbetalingDager) {
            println(dag)
        }

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[0].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[1].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[2].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[3].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[4].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[5].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[6].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[7].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[8].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[9].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[10].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[11].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[12].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[13].status shouldBe UtbetalingStatus.FullUtbetaling

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[14].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[15].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[16].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[17].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[18].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[19].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[20].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[21].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[22].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[23].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[24].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[25].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[26].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[27].status shouldBe UtbetalingStatus.FullUtbetaling
    }

    @Test
    fun `fravær med 16 dager i mellom gir fullUtbetaling`() {
        val meldekortId = UUID.randomUUID()
        val meldekortBeregning =
            MeldekortBeregning.beregnUtbetalingsDager(
                meldekortDager = listOf(
                    MeldekortDag(dato = 1.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 2.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 3.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 4.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 5.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 6.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 7.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 8.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 9.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 10.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 11.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 12.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 13.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 14.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),

                    MeldekortDag(dato = 15.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 16.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 17.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 18.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 19.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 20.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 21.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 22.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 23.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 24.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 25.oktober(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 26.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 27.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 28.oktober(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                ),
                saksbehandler = "saksbehandler",
                meldekortId = UUID.randomUUID(),
            )

        for (dag in meldekortBeregning.utbetalingDager) {
            println(dag)
        }

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[0].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[1].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[2].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[3].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[4].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[5].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[6].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[7].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[8].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[9].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[10].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[11].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[12].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[13].status shouldBe UtbetalingStatus.FullUtbetaling

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[14].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[15].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[16].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[17].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[18].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[19].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[20].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[21].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[22].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[23].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[24].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[25].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[26].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[27].status shouldBe UtbetalingStatus.FullUtbetaling
    }

    @Test
    fun `sjekk at 16ende dag etter forrige sykdom regnes med i samme arbeidsgiverperiode`() {
        // Eksempel 1 - To sykeperioder med 16 dagers mellomrom
        val meldekortId = UUID.randomUUID()
        val meldekortBeregning =
            MeldekortBeregning.beregnUtbetalingsDager(
                meldekortDager = listOf(
                    MeldekortDag(dato = 1.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 2.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 3.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 4.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 5.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 6.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 7.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 8.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 9.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 10.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 11.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 12.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 13.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 14.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 15.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 16.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 17.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 18.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 19.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 20.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 21.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 22.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 23.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 24.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 25.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 26.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                ),
                saksbehandler = "saksbehandler",
                meldekortId = UUID.randomUUID(),
            )
        // fravær syk 2 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[0].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[1].status shouldBe UtbetalingStatus.FullUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[2].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[3].status shouldBe UtbetalingStatus.IngenUtbetaling
        // fravær syk 1 dag
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[4].status shouldBe UtbetalingStatus.FullUtbetaling
        // deltakelse 4 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[5].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[6].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[7].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[8].status shouldBe UtbetalingStatus.FullUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[9].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[10].status shouldBe UtbetalingStatus.IngenUtbetaling
        // deltakelse 5 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[11].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[12].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[13].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[14].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[15].status shouldBe UtbetalingStatus.FullUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[16].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[17].status shouldBe UtbetalingStatus.IngenUtbetaling
        // deltakelse 2 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[18].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[19].status shouldBe UtbetalingStatus.FullUtbetaling
        // fravær syk 3 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[20].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[21].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[22].status shouldBe UtbetalingStatus.DelvisUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[23].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[24].status shouldBe UtbetalingStatus.IngenUtbetaling
        // fravær syk 1 dag
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[25].status shouldBe UtbetalingStatus.DelvisUtbetaling
    }

    @Test
    fun `sjekk at 17ende dag etter forrige sykdom gir ny arbeidsgiverperiode`() {
        // Eksempel 2 - To sykeperioder med 17 dagers mellomrom
        val meldekortId = UUID.randomUUID()
        val meldekortBeregning =
            MeldekortBeregning.beregnUtbetalingsDager(
                meldekortDager = listOf(
                    MeldekortDag(dato = 1.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 2.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 3.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 4.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 5.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 6.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 7.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 8.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 9.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 10.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 11.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 12.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 13.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 14.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 15.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 16.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 17.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 18.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 19.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 20.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 21.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 22.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 23.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 24.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 25.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 26.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                ),
                saksbehandler = "saksbehandler",
                meldekortId = UUID.randomUUID(),
            )
        // fravær syk 2 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[0].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[1].status shouldBe UtbetalingStatus.FullUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[2].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[3].status shouldBe UtbetalingStatus.IngenUtbetaling
        // fravær syk 1 dag
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[4].status shouldBe UtbetalingStatus.FullUtbetaling
        // deltakelse 4 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[5].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[6].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[7].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[8].status shouldBe UtbetalingStatus.FullUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[9].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[10].status shouldBe UtbetalingStatus.IngenUtbetaling
        // deltakelse 5 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[11].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[12].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[13].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[14].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[15].status shouldBe UtbetalingStatus.FullUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[16].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[17].status shouldBe UtbetalingStatus.IngenUtbetaling
        // deltakelse 2 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[18].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[19].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[20].status shouldBe UtbetalingStatus.FullUtbetaling
        // fravær syk 2 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[21].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[22].status shouldBe UtbetalingStatus.FullUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[23].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[24].status shouldBe UtbetalingStatus.IngenUtbetaling
        // fravær syk 1 dag
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[25].status shouldBe UtbetalingStatus.FullUtbetaling
    }

    @Test
    fun `sjekk at 17ende dag etter utbetalt arbeidsgiverperiode gir ny arbeidsgiverperiode`() {
        // Eksempel 3 - Utbetalt arbeidsgiverperiode - ny sykdom etter 17 dager
        val meldekortId = UUID.randomUUID()
        val meldekortBeregning =
            MeldekortBeregning.beregnUtbetalingsDager(
                meldekortDager = listOf(
                    MeldekortDag(dato = 1.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 2.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 3.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 4.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 5.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 6.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 7.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 8.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 9.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 10.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 11.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 12.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 13.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 14.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 15.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 16.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 17.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 18.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 19.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 20.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 21.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 22.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 23.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 24.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 25.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 26.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 27.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 28.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 29.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 1.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 2.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 3.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 4.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 5.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 6.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 7.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 8.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 9.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 10.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 11.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 12.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 13.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 14.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 15.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 16.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 17.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 18.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 19.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 20.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 21.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 22.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 23.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 24.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 25.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 26.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 27.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 28.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 29.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 30.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 31.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 1.april(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 2.april(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                ),
                saksbehandler = "saksbehandler",
                meldekortId = UUID.randomUUID(),
            )

        for (dag in meldekortBeregning.utbetalingDager) {
            println(dag)
        }

        // fravær syk 2 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[0].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[1].status shouldBe UtbetalingStatus.FullUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[2].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[3].status shouldBe UtbetalingStatus.IngenUtbetaling
        // fravær syk 5 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[4].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[5].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[6].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[7].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[8].status shouldBe UtbetalingStatus.DelvisUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[9].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[10].status shouldBe UtbetalingStatus.IngenUtbetaling
        // fravær syk 5 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[11].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[12].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[13].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[14].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[15].status shouldBe UtbetalingStatus.DelvisUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[16].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[17].status shouldBe UtbetalingStatus.IngenUtbetaling
        // uke med både deltakelse og fravær syk
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[18].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[19].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[20].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[21].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[22].status shouldBe UtbetalingStatus.DelvisUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[23].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[24].status shouldBe UtbetalingStatus.IngenUtbetaling
        // fravær syk 1 dag - til sammen 16 betalte sykedager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[25].status shouldBe UtbetalingStatus.DelvisUtbetaling
        // Nå starter nedtellingen til ny arbeidsgiverperiode
        // Nå følger en periode med deltakelse, ikke-deltakelse og sykt barn
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[26].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[27].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[28].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[29].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[30].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[31].status shouldBe UtbetalingStatus.IngenUtbetaling // søndag 3.3

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[32].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[33].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[34].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[35].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[36].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[37].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[38].status shouldBe UtbetalingStatus.IngenUtbetaling // søndag 10.3

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[39].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[40].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[41].status shouldBe UtbetalingStatus.FullUtbetaling
        // fravær syk - full utbetaling siden det har gått 17 dager siden fullt utbetalt arbeidsgiverperiode
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[42].status shouldBe UtbetalingStatus.FullUtbetaling

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[43].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[44].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[45].status shouldBe UtbetalingStatus.IngenUtbetaling // søndag 17.3

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[46].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[47].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[48].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[49].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[50].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[51].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[52].status shouldBe UtbetalingStatus.IngenUtbetaling // søndag 24.3

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[53].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[54].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[55].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[56].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[57].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[58].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[59].status shouldBe UtbetalingStatus.IngenUtbetaling // søndag 31.3

        // fravær syk - over 16 dager siden forrige sykdom
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[60].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[61].status shouldBe UtbetalingStatus.FullUtbetaling
    }

    @Test
    fun `sjekk at 16ende dag etter utbetalte arbeidsgiverperiode regnes med i samme arbeidsgiverperiode`() {
        // Eksempel 4 - Utbetalt arbeidsgiverperiode - ny sykdom etter 16 dager
        val meldekortId = UUID.randomUUID()
        val meldekortBeregning =
            MeldekortBeregning.beregnUtbetalingsDager(
                meldekortDager = listOf(
                    MeldekortDag(dato = 1.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 2.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 3.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 4.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 5.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 6.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 7.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 8.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 9.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 10.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 11.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 12.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 13.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 14.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 15.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 16.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 17.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 18.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 19.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 20.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 21.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 22.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 23.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 24.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 25.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 26.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 27.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 28.februar(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 29.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 1.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 2.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 3.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 4.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 5.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 6.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 7.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 8.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 9.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 10.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 11.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 12.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 13.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 14.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 15.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 16.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 17.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 18.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 19.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 20.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 21.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 22.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 23.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 24.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 25.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 26.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 27.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 28.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 29.mars(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 30.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 31.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),

                    MeldekortDag(dato = 1.april(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 2.april(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 3.april(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 4.april(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 5.april(), tiltak = gruppeAmo, status = DELTATT_UTEN_LØNN_I_TILTAKET, meldekortId),
                    MeldekortDag(dato = 6.april(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 7.april(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                ),
                saksbehandler = "saksbehandler",
                meldekortId = UUID.randomUUID(),
            )
        // fravær syk 2 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[0].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[1].status shouldBe UtbetalingStatus.FullUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[2].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[3].status shouldBe UtbetalingStatus.IngenUtbetaling
        // fravær syk 5 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[4].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[5].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[6].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[7].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[8].status shouldBe UtbetalingStatus.DelvisUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[9].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[10].status shouldBe UtbetalingStatus.IngenUtbetaling
        // fravær syk 5 dager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[11].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[12].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[13].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[14].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[15].status shouldBe UtbetalingStatus.DelvisUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[16].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[17].status shouldBe UtbetalingStatus.IngenUtbetaling
        // uke med både deltakelse og fravær syk
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[18].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[19].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[20].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[21].status shouldBe UtbetalingStatus.DelvisUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[22].status shouldBe UtbetalingStatus.DelvisUtbetaling
        // helg uten deltakelse
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[23].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[24].status shouldBe UtbetalingStatus.IngenUtbetaling
        // fravær syk 1 dag - til sammen 16 betalte sykedager
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[25].status shouldBe UtbetalingStatus.DelvisUtbetaling
        // Nå starter nedtellingen til ny arbeidsgiverperiode
        // Nå følger en periode med deltakelse, ikke-deltakelse og sykt barn
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[26].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[27].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[28].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[29].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[30].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[31].status shouldBe UtbetalingStatus.IngenUtbetaling // søndag 3.3

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[32].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[33].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[34].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[35].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[36].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[37].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[38].status shouldBe UtbetalingStatus.IngenUtbetaling // søndag 10.3

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[39].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[40].status shouldBe UtbetalingStatus.FullUtbetaling
        // fravær syk - 16 dager siden forrige sykefravær - det kreves 17 dager for å få full utbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[41].status shouldBe UtbetalingStatus.IngenUtbetaling

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[42].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[43].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[44].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[45].status shouldBe UtbetalingStatus.IngenUtbetaling // søndag 17.3

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[46].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[47].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[48].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[49].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[50].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[51].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[52].status shouldBe UtbetalingStatus.IngenUtbetaling // søndag 24.3

        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[53].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[54].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[55].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[56].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[57].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[58].status shouldBe UtbetalingStatus.IngenUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[59].status shouldBe UtbetalingStatus.IngenUtbetaling // søndag 31.3

        // fravær syk - over 16 dager siden forrige sykdom
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[53].status shouldBe UtbetalingStatus.FullUtbetaling
        meldekortBeregning.utbetalingDager.sortedBy { it.dag }[54].status shouldBe UtbetalingStatus.FullUtbetaling
    }

    private val gruppeAmo = Tiltak(
        id = UUID.randomUUID(),
        periode = Periode(fra = LocalDate.of(2024, 1, 1), til = LocalDate.of(2024, 1, 4)),
        tiltakstype = TiltakstypeSomGirRett.GRUPPE_AMO,
        antDagerIUken = 5,
    )
}

fun Int.januar(year: Int = 2024) = LocalDate.of(year, 1, this)
fun Int.oktober(year: Int = 2024) = LocalDate.of(year, 10, this)
fun Int.november(year: Int = 2024) = LocalDate.of(year, 11, this)
fun Int.desember(year: Int = 2024) = LocalDate.of(year, 12, this)
fun Int.februar(year: Int = 2024) = LocalDate.of(year, 2, this)
fun Int.mars(year: Int = 2024) = LocalDate.of(year, 3, this)
fun Int.april(year: Int = 2024) = LocalDate.of(year, 4, this)
