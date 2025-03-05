package no.nav.tiltakspenger.meldekort.repository

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.nå
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
    private fun lagreMeldekort(helper: TestDataHelper, vararg meldekort: Meldekort) {
        val meldeperiodeRepo = helper.meldeperiodeRepo
        val meldekortRepo = helper.meldekortPostgresRepo

        meldekort.forEach {
            meldeperiodeRepo.lagre(it.meldeperiode)
            meldekortRepo.lagre(it)
        }
    }

    @Nested
    inner class Lagre {
        @Test
        fun `lagres og kan hentes ut`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(mottatt = nå(), varselId = VarselId("varsel1"))

                lagreMeldekort(helper, meldekort)

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
                lagreMeldekort(helper, meldekort)

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
        private fun lagOgPersisterToMeldekort(helper: TestDataHelper, førstePeriode: Periode): List<Meldekort> {
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

            lagreMeldekort(helper, førsteMeldekort, andreMeldekort)

            return listOf(andreMeldekort, førsteMeldekort)
        }

        @Test
        fun `skal hente meldekort fra perioder som kan innsendes`() {
            withMigratedDb { helper ->
                val (sisteMeldekort, forrigeMeldekort) = lagOgPersisterToMeldekort(
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
            withMigratedDb { helper ->
                val (sisteMeldekort, forrigeMeldekort) = lagOgPersisterToMeldekort(
                    helper,
                    // TODO: kan vi mocke system-clock elns for å teste litt mer spesifikt på datoer?
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
            withMigratedDb { helper ->
                val (sisteMeldekort) = lagOgPersisterToMeldekort(
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

                lagreMeldekort(helper, meldekort1, meldekort2)

                val result = repo.hentMottatteSomDetVarslesFor()

                result.size shouldBe 2

                result[0].varselId shouldBe VarselId("varsel1")
                result[1].varselId shouldBe VarselId("varsel2")
            }
        }

        @Test
        fun `henter bare ut relevante meldekort`() {
            withMigratedDb { helper ->
                val meldekortRepo = helper.meldekortPostgresRepo
                val meldekort1 = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = VarselId("varsel1"))
                val meldekort2 = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = VarselId("varsel2"))
                val meldekort3 = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = null)
                val meldekort4 = ObjectMother.meldekort(mottatt = null, varselId = VarselId("varsel4"))

                lagreMeldekort(helper, meldekort1, meldekort2, meldekort3, meldekort4)

                val result = meldekortRepo.hentMottatteSomDetVarslesFor()

                result.size shouldBe 2

                result[0].varselId shouldBe VarselId("varsel1")
                result[1].varselId shouldBe VarselId("varsel2")
            }
        }
    }
}
