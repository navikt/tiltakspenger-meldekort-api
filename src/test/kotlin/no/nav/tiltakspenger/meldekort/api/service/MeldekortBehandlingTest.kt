package no.nav.tiltakspenger.meldekort.api.service

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.api.domene.Tiltak
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class MeldekortBehandlingTest {
    @Test
    fun eks() {
        val meldekortBehandling =
            MeldekortBehandling.lagBehandling(
                meldekortDager = listOf(
                    MeldekortDag(
                        dato = LocalDate.of(2024, 1, 29),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 1, 30),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 1, 31),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 1),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 2),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 3),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 4),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 5),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 6),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 7),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 8),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 9),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 10),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 11),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 12),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 13),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 14),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 15),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 16),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 17),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 18),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 19),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 20),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 21),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 22),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 23),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 24),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 25),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 26),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 27),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 28),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 2, 29),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 1),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 2),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 3),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 4),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 5),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 6),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 7),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 8),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 9),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 10),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 11),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 12),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYKT_BARN,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 13),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYKT_BARN,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 14),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 15),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 16),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 17),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 18),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 19),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYKT_BARN,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 20),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 21),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 22),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 23),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 24),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 25),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 26),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 27),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 28),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 29),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 30),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 31),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 1),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 2),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYK,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 3),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.FRAVÆR_SYKT_BARN,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 4),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 5),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 6),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                    MeldekortDag(
                        dato = LocalDate.of(2024, 3, 7),
                        tiltak = gruppeAmo,
                        status = MeldekortDagStatus.IKKE_DELTATT,
                    ),
                ),
                saksbehandler = "saksbehandler",
            )

        for (dag in meldekortBehandling.utbetalingDager) {
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
