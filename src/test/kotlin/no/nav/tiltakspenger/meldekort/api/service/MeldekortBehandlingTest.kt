package no.nav.tiltakspenger.meldekort.api.service

import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDagStatus
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class MeldekortBehandlingTest {
    @Test
    fun eks() {
        val meldekortBehandling =
            MeldekortBehandling(
                meldekort = listOf(
                    Meldekort.Åpent(
                        id = UUID.randomUUID(),
                        løpenr = 1,
                        fom = LocalDate.of(2024, 1, 29),
                        tom = LocalDate.of(2024, 2, 11),
                        forrigeMeldekort = null,
                        meldekortDager = listOf(
                            MeldekortDag(dato = LocalDate.of(2024, 1, 29), tiltak = null, status = MeldekortDagStatus.IKKE_DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 1, 30), tiltak = null, status = MeldekortDagStatus.IKKE_DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 1, 31), tiltak = null, status = MeldekortDagStatus.IKKE_DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 1), tiltak = null, status = MeldekortDagStatus.FRAVÆR_SYK),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 2), tiltak = null, status = MeldekortDagStatus.DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 3), tiltak = null, status = MeldekortDagStatus.IKKE_DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 4), tiltak = null, status = MeldekortDagStatus.IKKE_DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 5), tiltak = null, status = MeldekortDagStatus.DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 6), tiltak = null, status = MeldekortDagStatus.DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 7), tiltak = null, status = MeldekortDagStatus.DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 8), tiltak = null, status = MeldekortDagStatus.FRAVÆR_SYK),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 9), tiltak = null, status = MeldekortDagStatus.DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 10), tiltak = null, status = MeldekortDagStatus.IKKE_DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 11), tiltak = null, status = MeldekortDagStatus.IKKE_DELTATT),
                        ),
                        sistEndret = LocalDateTime.now(),
                        opprettet = LocalDateTime.now(),
                    ),
                    Meldekort.Åpent(
                        id = UUID.randomUUID(),
                        løpenr = 1,
                        fom = LocalDate.of(2024, 2, 12),
                        tom = LocalDate.of(2024, 2, 25),
                        forrigeMeldekort = null,
                        meldekortDager = listOf(
                            MeldekortDag(dato = LocalDate.of(2024, 2, 12), tiltak = null, status = MeldekortDagStatus.DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 13), tiltak = null, status = MeldekortDagStatus.DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 14), tiltak = null, status = MeldekortDagStatus.FRAVÆR_SYK),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 15), tiltak = null, status = MeldekortDagStatus.FRAVÆR_SYK),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 16), tiltak = null, status = MeldekortDagStatus.DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 17), tiltak = null, status = MeldekortDagStatus.IKKE_DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 18), tiltak = null, status = MeldekortDagStatus.IKKE_DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 19), tiltak = null, status = MeldekortDagStatus.DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 20), tiltak = null, status = MeldekortDagStatus.DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 21), tiltak = null, status = MeldekortDagStatus.DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 22), tiltak = null, status = MeldekortDagStatus.DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 23), tiltak = null, status = MeldekortDagStatus.DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 24), tiltak = null, status = MeldekortDagStatus.IKKE_DELTATT),
                            MeldekortDag(dato = LocalDate.of(2024, 2, 25), tiltak = null, status = MeldekortDagStatus.IKKE_DELTATT),
                        ),
                        sistEndret = LocalDateTime.now(),
                        opprettet = LocalDateTime.now(),
                    ),
                ),
            )

        val utbet = meldekortBehandling.lagUtbetalingsdager()
        for (dag in utbet) {
            println(dag)
        }
    }
}
