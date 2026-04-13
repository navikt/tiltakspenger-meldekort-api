package no.nav.tiltakspenger.meldekort.repository

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SendTilSaksbehandlingMeldekortRepoTest {

    @Nested
    inner class HentMeldekortForSendingTilSaksbehandling {
        @Test
        fun `returnerer mottatte og journalførte meldekort som ikke er sendt`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val meldekort = ObjectMother.meldekort(
                    mottatt = nå(fixedClockAt(1.mars(2025))),
                    periode = periode,
                )

                lagreMeldekort(helper, meldekort)

                // Først journalfør meldekortet
                repo.markerJournalført(
                    meldekort.id,
                    JournalpostId("jp-789"),
                    nå(fixedClock).truncatedTo(ChronoUnit.MICROS),
                )

                val result = repo.hentMeldekortForSendingTilSaksbehandling()

                result.size shouldBe 1
                result[0].id shouldBe meldekort.id
            }
        }

        @Test
        fun `ekskluderer meldekort uten journalpostId`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val meldekort = ObjectMother.meldekort(
                    mottatt = nå(fixedClockAt(1.mars(2025))),
                    periode = periode,
                )

                lagreMeldekort(helper, meldekort)

                // Ikke journalfør — journalpost_id er null
                val result = repo.hentMeldekortForSendingTilSaksbehandling()
                result shouldBe emptyList()
            }
        }

        @Test
        fun `ekskluderer ikke-mottatte meldekort`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val meldekort = ObjectMother.meldekort(
                    mottatt = null,
                    periode = periode,
                )

                lagreMeldekort(helper, meldekort)

                val result = repo.hentMeldekortForSendingTilSaksbehandling()
                result shouldBe emptyList()
            }
        }
    }

    @Nested
    inner class MarkerSendtTilSaksbehandling {
        @Test
        fun `meldekort forsvinner fra hentMeldekortForSendingTilSaksbehandling etter markering`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val meldekort = ObjectMother.meldekort(
                    mottatt = nå(fixedClockAt(1.mars(2025))),
                    periode = periode,
                )

                lagreMeldekort(helper, meldekort)
                repo.markerJournalført(
                    meldekort.id,
                    JournalpostId("jp-101"),
                    nå(fixedClock).truncatedTo(ChronoUnit.MICROS),
                )

                // Verifiser at meldekortet dukker opp før markering
                repo.hentMeldekortForSendingTilSaksbehandling().size shouldBe 1

                val sendtTidspunkt = nå(fixedClock).truncatedTo(ChronoUnit.MICROS)
                repo.markerSendtTilSaksbehandling(meldekort.id, sendtTidspunkt)

                // Verifiser at meldekortet ikke lenger dukker opp
                repo.hentMeldekortForSendingTilSaksbehandling() shouldBe emptyList()
            }
        }

        @Test
        fun `markerer kun spesifikt meldekort som sendt`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val meldekort1 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = nå(fixedClockAt(1.mars(2025))),
                    periode = førstePeriode,
                )
                val meldekort2 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = nå(fixedClockAt(1.mars(2025))),
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
                    ),
                )

                lagreMeldekort(helper, meldekort1, meldekort2)

                repo.markerJournalført(
                    meldekort1.id,
                    JournalpostId("jp-201"),
                    nå(fixedClock).truncatedTo(ChronoUnit.MICROS),
                )
                repo.markerJournalført(
                    meldekort2.id,
                    JournalpostId("jp-202"),
                    nå(fixedClock).truncatedTo(ChronoUnit.MICROS),
                )

                repo.markerSendtTilSaksbehandling(
                    meldekort1.id,
                    nå(fixedClock).truncatedTo(ChronoUnit.MICROS),
                )

                val result = repo.hentMeldekortForSendingTilSaksbehandling()
                result.size shouldBe 1
                result[0].id shouldBe meldekort2.id
            }
        }
    }
}
