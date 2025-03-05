package no.nav.tiltakspenger.meldekort.repository

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.time.LocalDate
import java.time.LocalDateTime

class MeldekortPostgresRepoTest {
    @Nested
    inner class Lagre {
        @Test
        fun `lagres og kan hentes ut`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo

                val meldekort = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = VarselId("varsel1"))
                repo.lagre(meldekort)

                val result = repo.hentForMeldekortId(meldekort.id, meldekort.meldeperiode.fnr)

                assertNotNull(result)
                assertEquals(meldekort.id, result.id, "id")
                assertEquals(meldekort.meldeperiode.id, result.meldeperiode.id, "meldeperiode.id")
                assertEquals(meldekort.mottatt, result.mottatt, "mottatt")
                assertEquals(meldekort.varselId, result.varselId, "varselId")
            }
        }
    }

    @Nested
    inner class Oppdater {
        @Test
        fun `kan oppdatere varselId`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo

                val meldekort = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = VarselId("varsel1"))
                repo.lagre(meldekort)
                assertNotNull(meldekort.varselId, "varselId skal være satt til å begynne med")

                val oppdatertMeldekort = meldekort.copy(varselId = null)
                repo.oppdater(oppdatertMeldekort)

                val result = repo.hentForMeldekortId(meldekort.id, meldekort.meldeperiode.fnr)
                assertEquals(oppdatertMeldekort.varselId, result!!.varselId, "varselId")
            }
        }
    }

    @Nested
    inner class HentMeldekortTilBruker {
        private fun lagToMeldekort(helper: TestDataHelper, førstePeriode: Periode): List<Meldekort> {
            val meldekortRepo = helper.meldekortPostgresRepo
            val meldeperiodeRepo = helper.meldeperiodeRepo

            val førsteMeldekort = ObjectMother.meldekort(
                mottatt = førstePeriode.tilOgMed.atTime(0, 0),
                periode = førstePeriode,
            )

            val andreMeldekort = ObjectMother.meldekort(
                mottatt = null,
                periode = Periode(
                    fraOgMed = førstePeriode.fraOgMed.plusDays(14),
                    tilOgMed = førstePeriode.tilOgMed.plusDays(14),
                ),
            )

            meldeperiodeRepo.lagre(førsteMeldekort.meldeperiode)
            meldeperiodeRepo.lagre(andreMeldekort.meldeperiode)

            meldekortRepo.lagre(førsteMeldekort)
            meldekortRepo.lagre(andreMeldekort)

            return listOf(andreMeldekort, førsteMeldekort)
        }

        @Test
        fun `skal hente meldekort fra perioder som kan innsendes`() {
            withMigratedDb(runIsolated = true) { helper ->
                val (sisteMeldekort, forrigeMeldekort) = lagToMeldekort(
                    helper,
                    Periode(
                        fraOgMed = LocalDate.of(2025, 1, 6),
                        tilOgMed = LocalDate.of(2025, 1, 19),
                    ),
                )

                val repo = helper.meldekortPostgresRepo

                val sisteMeldekortFraDb = repo.hentSisteMeldekort(sisteMeldekort.fnr)
                val alleMeldekortFraDb = repo.hentAlleMeldekort(sisteMeldekort.fnr)

                sisteMeldekortFraDb shouldBe sisteMeldekort
                alleMeldekortFraDb shouldBe listOf(sisteMeldekort, forrigeMeldekort)
            }
        }

        @Test
        fun `skal hente forrige meldekort når det nyeste ikke er klart til innsending`() {
            withMigratedDb(runIsolated = true) { helper ->
                val (sisteMeldekort, forrigeMeldekort) = lagToMeldekort(
                    helper,
                    ObjectMother.periode(LocalDate.now().minusDays(14)),
                )

                val repo = helper.meldekortPostgresRepo

                val sisteMeldekortFraDb = repo.hentSisteMeldekort(sisteMeldekort.fnr)
                val alleMeldekortFraDb = repo.hentAlleMeldekort(sisteMeldekort.fnr)

                sisteMeldekortFraDb shouldBe forrigeMeldekort
                alleMeldekortFraDb shouldBe listOf(forrigeMeldekort)
            }
        }

        @Test
        fun `skal ikke hente noen meldekort når ingen er klare til innsending`() {
            withMigratedDb(runIsolated = true) { helper ->
                val (sisteMeldekort) = lagToMeldekort(
                    helper,
                    ObjectMother.periode(LocalDate.now()),
                )

                val repo = helper.meldekortPostgresRepo

                val sisteMeldekortFraDb = repo.hentSisteMeldekort(sisteMeldekort.fnr)
                val alleMeldekortFraDb = repo.hentAlleMeldekort(sisteMeldekort.fnr)

                sisteMeldekortFraDb shouldBe null
                alleMeldekortFraDb shouldBe emptyList()
            }
        }
    }

    @Nested
    inner class HentMottatteSomDetVarslesFor {
        @Test
        fun `alle matcher kriteriene`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo

                val meldekort1 = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = VarselId("varsel1"))
                val meldekort2 = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = VarselId("varsel2"))

                repo.lagre(meldekort1)
                repo.lagre(meldekort2)

                val result = repo.hentMottatteSomDetVarslesFor()

                assertEquals(2, result.size, "Antall relevante meldekort")
                assertEquals("varsel1", result[0].varselId, "varselId")
                assertEquals("varsel2", result[1].varselId, "varselId")
            }
        }

        @Test
        fun `henter bare ut relevante meldekort`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort1 = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = VarselId("varsel1"))
                val meldekort2 = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = VarselId("varsel2"))
                val meldekort3 = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = null)
                val meldekort4 = ObjectMother.meldekort(mottatt = null, varselId = VarselId("varsel4"))

                repo.lagre(meldekort1)
                repo.lagre(meldekort2)
                repo.lagre(meldekort3)
                repo.lagre(meldekort4)

                val result = repo.hentMottatteSomDetVarslesFor()

                assertEquals(2, result.size, "Antall relevante meldekort")
                assertEquals("varsel1", result[0].varselId, "varselId")
                assertEquals("varsel2", result[1].varselId, "varselId")
            }
        }
    }
}
