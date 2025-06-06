package no.nav.tiltakspenger.meldekort.repository

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
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
            meldekortRepo.opprett(it)
        }
    }

    @Nested
    inner class Lagre {
        @Test
        fun `lagres og kan hentes ut`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(mottatt = nå(fixedClock), varselId = VarselId("varsel1"))

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
    inner class HentMeldekort {
        @Test
        fun `skal hente meldekort som kan utfylles og forrige innsendte meldekort`() {
            withMigratedDb { helper ->
                val førstePeriode = Periode(
                    fraOgMed = LocalDate.of(2025, 1, 6),
                    tilOgMed = LocalDate.of(2025, 1, 19),
                )

                val førsteMeldekort = ObjectMother.meldekort(
                    mottatt = førstePeriode.tilOgMed.atTime(0, 0),
                    periode = førstePeriode,
                )

                val andreMeldekort = ObjectMother.meldekort(
                    mottatt = null,
                    periode = førstePeriode.plus14Dager(),
                )

                lagreMeldekort(helper, førsteMeldekort, andreMeldekort)

                val repo = helper.meldekortPostgresRepo

                val fnr = førsteMeldekort.fnr

                val sisteMeldekortFraDb = repo.hentSisteUtfylteMeldekort(fnr)
                val nesteMeldekortFraDb = repo.hentNesteMeldekortTilUtfylling(fnr)
                val alleMeldekortFraDb = repo.hentAlleMeldekortForBruker(fnr)

                sisteMeldekortFraDb shouldBe førsteMeldekort
                nesteMeldekortFraDb shouldBe andreMeldekort
                alleMeldekortFraDb shouldBe listOf(andreMeldekort, førsteMeldekort)
            }
        }

        @Test
        fun `skal hente første meldekort når det andre ikke er klart til utfylling`() {
            withMigratedDb { helper ->
                val førstePeriode = ObjectMother.periode(LocalDate.now().minusWeeks(2))

                val førsteMeldekort = ObjectMother.meldekort(
                    mottatt = null,
                    periode = førstePeriode,
                )

                val andreMeldekort = ObjectMother.meldekort(
                    mottatt = null,
                    periode = førstePeriode.plus14Dager(),
                )

                lagreMeldekort(helper, førsteMeldekort, andreMeldekort)

                val repo = helper.meldekortPostgresRepo

                val fnr = førsteMeldekort.fnr

                val sisteMeldekortFraDb = repo.hentSisteUtfylteMeldekort(fnr)
                val nesteMeldekortFraDb = repo.hentNesteMeldekortTilUtfylling(fnr)
                val alleMeldekortFraDb = repo.hentAlleMeldekortForBruker(fnr)

                sisteMeldekortFraDb shouldBe null
                nesteMeldekortFraDb shouldBe førsteMeldekort
                nesteMeldekortFraDb!!.status shouldBe MeldekortStatus.KAN_UTFYLLES
                alleMeldekortFraDb shouldBe listOf(førsteMeldekort)
            }
        }

        @Test
        fun `skal hente meldekort som ikke er klart til utfylling`() {
            withMigratedDb { helper ->
                val førstePeriode = ObjectMother.periode(LocalDate.now())

                val førsteMeldekort = ObjectMother.meldekort(
                    mottatt = null,
                    periode = førstePeriode,
                )

                val andreMeldekort = ObjectMother.meldekort(
                    mottatt = null,
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
                    ),
                )

                lagreMeldekort(helper, førsteMeldekort, andreMeldekort)

                val repo = helper.meldekortPostgresRepo

                val fnr = førsteMeldekort.fnr

                val sisteMeldekortFraDb = repo.hentSisteUtfylteMeldekort(fnr)
                val nesteMeldekortFraDb = repo.hentNesteMeldekortTilUtfylling(fnr)
                val alleMeldekortFraDb = repo.hentAlleMeldekortForBruker(fnr)

                sisteMeldekortFraDb shouldBe null
                nesteMeldekortFraDb shouldBe førsteMeldekort
                nesteMeldekortFraDb!!.status shouldBe MeldekortStatus.IKKE_KLAR
                alleMeldekortFraDb shouldBe emptyList()
            }
        }

        @Test
        fun `skal ikke hente meldekort til utfylling når alle er mottatt`() {
            withMigratedDb { helper ->
                val førstePeriode = Periode(
                    fraOgMed = LocalDate.of(2025, 1, 6),
                    tilOgMed = LocalDate.of(2025, 1, 19),
                )

                val førsteMeldekort = ObjectMother.meldekort(
                    mottatt = nå(fixedClock).minusWeeks(2),
                    periode = førstePeriode,
                )

                val andreMeldekort = ObjectMother.meldekort(
                    mottatt = nå(fixedClock),
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
                    ),
                )

                lagreMeldekort(helper, førsteMeldekort, andreMeldekort)

                val repo = helper.meldekortPostgresRepo

                val fnr = førsteMeldekort.fnr

                val sisteMeldekortFraDb = repo.hentSisteUtfylteMeldekort(fnr)
                val nesteMeldekortFraDb = repo.hentNesteMeldekortTilUtfylling(fnr)
                val alleMeldekortFraDb = repo.hentAlleMeldekortForBruker(fnr)

                sisteMeldekortFraDb shouldBe andreMeldekort
                nesteMeldekortFraDb shouldBe null
                alleMeldekortFraDb shouldBe listOf(andreMeldekort, førsteMeldekort)
            }
        }

        @Test
        fun `skal hente det første meldekortet som neste når flere er klare til utfylling`() {
            withMigratedDb { helper ->
                val førstePeriode = Periode(
                    fraOgMed = LocalDate.of(2025, 1, 6),
                    tilOgMed = LocalDate.of(2025, 1, 19),
                )

                val førsteMeldekort = ObjectMother.meldekort(
                    mottatt = null,
                    periode = førstePeriode,
                )

                val andreMeldekort = ObjectMother.meldekort(
                    mottatt = null,
                    periode = førstePeriode.plus14Dager(),
                )

                lagreMeldekort(helper, førsteMeldekort, andreMeldekort)

                val repo = helper.meldekortPostgresRepo

                val fnr = førsteMeldekort.fnr

                val sisteMeldekortFraDb = repo.hentSisteUtfylteMeldekort(fnr)
                val nesteMeldekortFraDb = repo.hentNesteMeldekortTilUtfylling(fnr)
                val alleMeldekortFraDb = repo.hentAlleMeldekortForBruker(fnr)

                sisteMeldekortFraDb shouldBe null
                nesteMeldekortFraDb shouldBe førsteMeldekort
                alleMeldekortFraDb shouldBe listOf(andreMeldekort, førsteMeldekort)
            }
        }
    }

    @Nested
    inner class HentMeldekortDetSkalVarslesFor {
        @Test
        fun `alle matcher kriteriene`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo

                val førstePeriode = Periode(
                    fraOgMed = LocalDate.of(2025, 1, 6),
                    tilOgMed = LocalDate.of(2025, 1, 19),
                )

                val meldekort1 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = null,
                    varselId = null,
                    erVarselInaktivert = false,
                    periode = førstePeriode,
                )
                val meldekort2 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = null,
                    varselId = null,
                    erVarselInaktivert = false,
                    periode = førstePeriode.plus14Dager(),
                )

                lagreMeldekort(helper, meldekort1, meldekort2)

                val result = repo.hentMeldekortDetSkalVarslesFor().sortedBy { it.periode.fraOgMed }

                result.size shouldBe 2

                result[0].id shouldBe meldekort1.id
                result[1].id shouldBe meldekort2.id
            }
        }

        @Test
        fun `henter bare ut relevante meldekort`() {
            withMigratedDb { helper ->
                val meldekortRepo = helper.meldekortPostgresRepo

                val førstePeriode = Periode(
                    fraOgMed = LocalDate.of(2025, 1, 6),
                    tilOgMed = LocalDate.of(2025, 1, 19),
                )

                val meldekort1 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = null,
                    varselId = null,
                    erVarselInaktivert = false,
                    periode = førstePeriode,
                )
                val meldekort2 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = null,
                    varselId = VarselId("Varsel-meldekort2"),
                    erVarselInaktivert = false,
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
                    ),
                )
                val meldekort3 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = null,
                    varselId = VarselId("Varsel-meldekort3"),
                    erVarselInaktivert = true,
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(4),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(4),
                    ),
                )
                val meldekort4 = ObjectMother.meldekort(
                    fnr = Fnr.random(),
                    mottatt = LocalDateTime.now(),
                    varselId = VarselId("Varsel-meldekort4"),
                    erVarselInaktivert = true,
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(6),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(6),
                    ),
                )

                lagreMeldekort(helper, meldekort1, meldekort2, meldekort3, meldekort4)

                val result = meldekortRepo.hentMeldekortDetSkalVarslesFor()

                result.size shouldBe 1

                result[0].id shouldBe meldekort1.id
            }
        }

        @Test
        fun `henter ikke meldekort hvis vi har sendt varsel for forrige meldekort som ikke er mottatt`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo

                val fnr = Fnr.random()
                val førstePeriode = Periode(
                    fraOgMed = LocalDate.of(2025, 1, 6),
                    tilOgMed = LocalDate.of(2025, 1, 19),
                )

                val meldekort1 = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    varselId = VarselId.random(),
                    erVarselInaktivert = false,
                    periode = førstePeriode,
                )
                val meldekort2 = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    varselId = null,
                    erVarselInaktivert = false,
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
                    ),
                )

                lagreMeldekort(helper, meldekort1, meldekort2)

                val result = repo.hentMeldekortDetSkalVarslesFor()

                result.size shouldBe 0
            }
        }

        @Test
        fun `henter neste meldekort hvis forrige meldekort er mottatt`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo

                val fnr = Fnr.random()
                val førstePeriode = Periode(
                    fraOgMed = LocalDate.of(2025, 1, 6),
                    tilOgMed = LocalDate.of(2025, 1, 19),
                )

                val meldekort1 = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = LocalDateTime.now(),
                    varselId = VarselId.random(),
                    erVarselInaktivert = true,
                    periode = førstePeriode,
                )
                val meldekort2 = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    varselId = null,
                    erVarselInaktivert = false,
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
                    ),
                )

                val meldekort3 = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    varselId = null,
                    erVarselInaktivert = false,
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(4),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(4),
                    ),
                )

                lagreMeldekort(helper, meldekort1, meldekort2, meldekort3)

                val result = repo.hentMeldekortDetSkalVarslesFor()

                result.size shouldBe 1
                result[0].id shouldBe meldekort2.id
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

                val result = repo.hentMottatteEllerDeaktiverteSomDetVarslesFor()

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
                val meldekort3 = ObjectMother.meldekort(
                    mottatt = LocalDateTime.now(),
                    varselId = VarselId("varsel4"),
                    erVarselInaktivert = true,
                )
                val meldekort4 = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = null)
                val meldekort5 = ObjectMother.meldekort(mottatt = null, varselId = VarselId("varsel4"))

                lagreMeldekort(helper, meldekort1, meldekort2, meldekort3, meldekort4, meldekort5)

                val result = meldekortRepo.hentMottatteEllerDeaktiverteSomDetVarslesFor()

                result.size shouldBe 2

                result[0].varselId shouldBe VarselId("varsel1")
                result[1].varselId shouldBe VarselId("varsel2")
            }
        }
    }
}
