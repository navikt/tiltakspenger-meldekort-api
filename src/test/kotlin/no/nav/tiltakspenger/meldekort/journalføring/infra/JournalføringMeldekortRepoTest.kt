package no.nav.tiltakspenger.meldekort.journalføring.infra

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.journalføring.JournalpostId
import no.nav.tiltakspenger.meldekort.meldekort.infra.lagreMeldekort
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

class JournalføringMeldekortRepoTest {

    @Nested
    inner class MarkerJournalført {
        @Test
        fun `setter journalpostId og tidspunkt`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val journalføringRepo = helper.journalføringPostgresRepo
                val meldekortRepo = helper.meldekortPostgresRepo
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val meldekort = ObjectMother.meldekort(
                    mottatt = nå(fixedClockAt(1.mars(2025))),
                    periode = periode,
                )

                lagreMeldekort(helper, meldekort)

                val journalpostId = JournalpostId("jp-123")
                val tidspunkt = nå(fixedClock).truncatedTo(ChronoUnit.MICROS)

                journalføringRepo.markerJournalført(meldekort.id, journalpostId, tidspunkt)

                val result = meldekortRepo.hentForMeldekortId(meldekort.id, meldekort.fnr)

                result shouldNotBe null
                result!!.journalpostId shouldBe journalpostId
                result.journalføringstidspunkt shouldBe tidspunkt
            }
        }
    }

    @Nested
    inner class HentDeSomSkalJournalføres {
        @Test
        fun `returnerer mottatte meldekort uten journalpostId`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val journalføringRepo = helper.journalføringPostgresRepo
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val mottattMeldekort = ObjectMother.meldekort(
                    mottatt = nå(fixedClockAt(1.mars(2025))),
                    periode = periode,
                )

                lagreMeldekort(helper, mottattMeldekort)

                val result = journalføringRepo.hentDeSomSkalJournalføres()

                result.size shouldBe 1
                result[0].id shouldBe mottattMeldekort.id
                result[0].journalpostId shouldBe null
            }
        }

        @Test
        fun `ekskluderer ikke-mottatte meldekort`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val journalføringRepo = helper.journalføringPostgresRepo
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val ikkeMottattMeldekort = ObjectMother.meldekort(
                    mottatt = null,
                    periode = periode,
                )

                lagreMeldekort(helper, ikkeMottattMeldekort)

                val result = journalføringRepo.hentDeSomSkalJournalføres()
                result shouldBe emptyList()
            }
        }

        @Test
        fun `ekskluderer allerede journalførte meldekort`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val journalføringRepo = helper.journalføringPostgresRepo
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val meldekort = ObjectMother.meldekort(
                    mottatt = nå(fixedClockAt(1.mars(2025))),
                    periode = periode,
                )

                lagreMeldekort(helper, meldekort)

                journalføringRepo.markerJournalført(meldekort.id, JournalpostId("jp-456"), nå(fixedClock).truncatedTo(ChronoUnit.MICROS))

                val result = journalføringRepo.hentDeSomSkalJournalføres()
                result shouldBe emptyList()
            }
        }

        @Test
        fun `respekterer limit`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val journalføringRepo = helper.journalføringPostgresRepo
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
                val meldekort3 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = nå(fixedClockAt(1.mars(2025))),
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(4),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(4),
                    ),
                )

                lagreMeldekort(helper, meldekort1, meldekort2, meldekort3)

                val result = journalføringRepo.hentDeSomSkalJournalføres(limit = 2)
                result.size shouldBe 2
            }
        }
    }
}
