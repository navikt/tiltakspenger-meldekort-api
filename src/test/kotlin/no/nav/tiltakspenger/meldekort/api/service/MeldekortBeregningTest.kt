package no.nav.tiltakspenger.meldekort.api.service

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortBeregning
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.DELTATT
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.FRAVÆR_SYK
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.FRAVÆR_SYKT_BARN
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus.IKKE_DELTATT
import no.nav.tiltakspenger.meldekort.api.domene.Tiltak
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class MeldekortBeregningTest {
    @Test
    fun eks() {
        val meldekortId = UUID.randomUUID()
        val meldekortBeregning =
            MeldekortBeregning.beregn(
                meldekortDager = listOf(
                    MeldekortDag(dato = 29.januar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 30.januar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 31.januar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 1.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 2.februar(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 3.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 4.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 5.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 6.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 7.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 8.februar(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 9.februar(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 10.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 11.februar(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 12.februar(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 13.februar(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
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
                    MeldekortDag(dato = 5.mars(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 6.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 7.mars(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 8.mars(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 9.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 10.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 11.mars(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 12.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 13.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 14.mars(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 15.mars(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 16.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 17.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 18.mars(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 19.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 20.mars(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 21.mars(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 22.mars(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 23.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 24.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 25.mars(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 26.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 27.mars(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 28.mars(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 29.mars(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 30.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 31.mars(), tiltak = gruppeAmo, status = IKKE_DELTATT, meldekortId),
                    MeldekortDag(dato = 1.april(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 2.april(), tiltak = gruppeAmo, status = FRAVÆR_SYK, meldekortId),
                    MeldekortDag(dato = 3.april(), tiltak = gruppeAmo, status = FRAVÆR_SYKT_BARN, meldekortId),
                    MeldekortDag(dato = 4.april(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
                    MeldekortDag(dato = 5.april(), tiltak = gruppeAmo, status = DELTATT, meldekortId),
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

    private val gruppeAmo = Tiltak(
        id = UUID.randomUUID(),
        periode = Periode(fra = LocalDate.of(2024, 1, 1), til = LocalDate.of(2024, 1, 4)),
        typeBeskrivelse = "",
        typeKode = "GRUPPEAMO",
        antDagerIUken = 0.0f,
    )
}

fun Int.januar(year: Int = 2024) = LocalDate.of(year, 1, this)
fun Int.februar(year: Int = 2024) = LocalDate.of(year, 2, this)
fun Int.mars(year: Int = 2024) = LocalDate.of(year, 3, this)
fun Int.april(year: Int = 2024) = LocalDate.of(year, 4, this)
