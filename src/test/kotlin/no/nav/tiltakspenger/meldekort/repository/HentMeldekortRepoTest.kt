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
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.domene.MeldekortMedSisteMeldeperiode
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

class HentMeldekortRepoTest {

    @Nested
    inner class HentSisteUtfylteMeldekort {
        @Test
        fun `returnerer siste mottatte meldekort`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val førsteMeldekort = ObjectMother.meldekort(
                    mottatt = førstePeriode.tilOgMed.atTime(0, 0),
                    periode = førstePeriode,
                )
                val andreMeldekort = ObjectMother.meldekort(
                    mottatt = null,
                    periode = førstePeriode.plus14Dager(),
                )

                lagreMeldekort(helper, førsteMeldekort, andreMeldekort)

                val result = repo.hentSisteUtfylteMeldekort(førsteMeldekort.fnr)
                result shouldBe førsteMeldekort
            }
        }

        @Test
        fun `returnerer null når ingen meldekort er mottatt`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(mottatt = null)

                lagreMeldekort(helper, meldekort)

                val result = repo.hentSisteUtfylteMeldekort(meldekort.fnr)
                result shouldBe null
            }
        }

        @Test
        fun `returnerer nyeste mottatte meldekort når flere er mottatt`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

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

                val result = repo.hentSisteUtfylteMeldekort(førsteMeldekort.fnr)
                result shouldBe andreMeldekort
            }
        }
    }

    @Nested
    inner class HentNesteMeldekortTilUtfylling {
        @Test
        fun `skal hente meldekort som kan utfylles og forrige innsendte meldekort`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

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
                val alleInnsendteMeldekort = repo.hentInnsendteMeldekortForBruker(fnr)

                sisteMeldekortFraDb shouldBe førsteMeldekort
                nesteMeldekortFraDb shouldBe andreMeldekort
                alleInnsendteMeldekort shouldBe listOf(MeldekortMedSisteMeldeperiode(førsteMeldekort, førsteMeldekort.meldeperiode))
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

                sisteMeldekortFraDb shouldBe null
                nesteMeldekortFraDb shouldBe førsteMeldekort
                nesteMeldekortFraDb!!.status(
                    clock = fixedClockAt(førstePeriode.tilOgMed),
                ) shouldBe MeldekortStatus.KAN_UTFYLLES
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
                val alleMeldekortFraDb = repo.hentInnsendteMeldekortForBruker(fnr)

                sisteMeldekortFraDb shouldBe null
                nesteMeldekortFraDb shouldBe førsteMeldekort
                nesteMeldekortFraDb!!.status(
                    clock = fixedClockAt(førstePeriode.fraOgMed),
                ) shouldBe MeldekortStatus.IKKE_KLAR
                alleMeldekortFraDb shouldBe emptyList()
            }
        }

        @Test
        fun `skal ikke hente meldekort til utfylling når alle er mottatt`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

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
                val alleMeldekortFraDb = repo.hentInnsendteMeldekortForBruker(fnr)

                sisteMeldekortFraDb shouldBe andreMeldekort
                nesteMeldekortFraDb shouldBe null
                alleMeldekortFraDb shouldBe listOf(
                    MeldekortMedSisteMeldeperiode(andreMeldekort, andreMeldekort.meldeperiode),
                    MeldekortMedSisteMeldeperiode(førsteMeldekort, førsteMeldekort.meldeperiode),
                )
            }
        }

        @Test
        fun `skal hente det første meldekortet som neste når flere er klare til utfylling`() {
            withMigratedDb { helper ->
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

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
                val alleInnsendteMeldekort = repo.hentInnsendteMeldekortForBruker(fnr)

                sisteMeldekortFraDb shouldBe null
                nesteMeldekortFraDb shouldBe førsteMeldekort
                alleInnsendteMeldekort shouldBe emptyList()
            }
        }
    }

    @Nested
    inner class HentInnsendteMeldekortForBruker {
        @Test
        fun `skal hente nyeste meldeperiode for innsendt meldekort`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val meldekort = ObjectMother.meldekort(
                    mottatt = nå(fixedClock).minusWeeks(2),
                    periode = periode,
                )

                lagreMeldekort(helper, meldekort)

                val oppdatertMeldeperiode = meldekort.meldeperiode.copy(
                    id = MeldeperiodeId.random(),
                    versjon = 2,
                    opprettet = nå(fixedClock).plusHours(1),
                )
                helper.meldeperiodeRepo.lagre(oppdatertMeldeperiode)

                val repo = helper.meldekortPostgresRepo
                val fnr = meldekort.fnr

                val innsendteMeldekort = repo.hentInnsendteMeldekortForBruker(fnr)

                innsendteMeldekort shouldBe listOf(
                    MeldekortMedSisteMeldeperiode(meldekort, oppdatertMeldeperiode),
                )
            }
        }

        @Test
        fun `returnerer tom liste når ingen meldekort er innsendt`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(mottatt = null)

                lagreMeldekort(helper, meldekort)

                val result = repo.hentInnsendteMeldekortForBruker(meldekort.fnr)
                result shouldBe emptyList()
            }
        }

        @Test
        fun `returnerer journalpostId for journalført innsendt meldekort`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val meldekort = ObjectMother.meldekort(
                    mottatt = nå(fixedClock),
                    periode = periode,
                )
                val journalpostId = no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId("jp-999")
                val journalføringstidspunkt = nå(fixedClock)

                lagreMeldekort(helper, meldekort)
                repo.markerJournalført(meldekort.id, journalpostId, journalføringstidspunkt)

                val result = repo.hentInnsendteMeldekortForBruker(meldekort.fnr)

                result.single().meldekort.journalpostId shouldBe journalpostId
                result.single().meldekort.journalføringstidspunkt shouldBe journalføringstidspunkt
            }
        }

        @Test
        fun `returnerer ikke deaktiverte meldekort`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val meldekort = ObjectMother.meldekort(
                    mottatt = null,
                    periode = periode,
                )

                lagreMeldekort(helper, meldekort)

                repo.deaktiver(meldekort.id)

                val result = repo.hentInnsendteMeldekortForBruker(meldekort.fnr)
                result shouldBe emptyList()
            }
        }
    }

    @Nested
    inner class HentSisteMeldekortForKjedeId {
        @Test
        fun `henter siste meldekort for en kjede`() {
            withMigratedDb { helper ->
                val meldekort = ObjectMother.meldekort()
                val kjedeId = meldekort.meldeperiode.kjedeId
                helper.meldeperiodeRepo.lagre(meldekort.meldeperiode)
                helper.meldekortPostgresRepo.lagre(meldekort)
                val hentetMeldekort = helper.meldekortPostgresRepo.hentSisteMeldekortForKjedeId(kjedeId, meldekort.fnr)
                hentetMeldekort shouldBe meldekort
            }
        }

        @Test
        fun `returnerer null for ukjent kjedeId`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = Fnr.random()
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val ukjentKjedeId = no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId.fraPeriode(periode)

                val result = repo.hentSisteMeldekortForKjedeId(ukjentKjedeId, fnr)
                result shouldBe null
            }
        }
    }

    @Nested
    inner class HentAlleMeldekortKlarTilInnsending {
        @Test
        fun `henter alle meldekort bruker kan fylle ut`() {
            val clock = fixedClockAt(1.mars(2025))
            withMigratedDb(clock = clock) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = Fnr.random()
                val nærmesteSøndag = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

                val nærmesteMeldekort = lagMeldekort(fnr, nærmesteSøndag)
                val forrigeForrigeMeldekort = lagMeldekort(fnr, nærmesteSøndag.minusWeeks(4))
                val forrigeMeldekort = lagMeldekort(fnr, nærmesteSøndag.minusWeeks(2))
                val nesteMeldekort = lagMeldekort(fnr, nærmesteSøndag.plusWeeks(2))
                val innsendtMeldekort = lagMeldekort(fnr, nærmesteSøndag.minusWeeks(2), mottatt = nå(fixedClockAt(nærmesteSøndag.minusWeeks(2))))
                val annenBrukersMeldekort = lagMeldekort(Fnr.random(), nærmesteSøndag)

                lagreMeldekort(helper, nærmesteMeldekort, forrigeForrigeMeldekort, forrigeMeldekort, nesteMeldekort, innsendtMeldekort, annenBrukersMeldekort)

                nærmesteMeldekort.klarTilInnsending(clock = clock) shouldBe true
                forrigeForrigeMeldekort.klarTilInnsending(clock = clock) shouldBe true
                forrigeMeldekort.klarTilInnsending(clock = clock) shouldBe true
                nesteMeldekort.klarTilInnsending(clock = clock) shouldBe false
                innsendtMeldekort.klarTilInnsending(clock = clock) shouldBe false

                val resultat = repo.hentAlleMeldekortKlarTilInnsending(fnr)

                resultat.size shouldBe 3
                resultat.map { it.periode.tilOgMed } shouldBe listOf(
                    forrigeForrigeMeldekort.periode.tilOgMed,
                    forrigeMeldekort.periode.tilOgMed,
                    nærmesteMeldekort.periode.tilOgMed,
                ).sorted()
                resultat.forEach {
                    it.status(fixedClockAt(it.periode.tilOgMed)) shouldBe MeldekortStatus.KAN_UTFYLLES
                    it.klarTilInnsending(fixedClockAt(it.periode.tilOgMed)) shouldBe true
                }
            }
        }

        @Test
        fun `returnerer tom liste når ingen meldekort er klare til innsending`() {
            val clock = fixedClockAt(1.mars(2025))
            withMigratedDb(clock = clock) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = Fnr.random()
                val fremtidigSøndag = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).plusWeeks(4)

                val fremtidigMeldekort = lagMeldekort(fnr, fremtidigSøndag)
                lagreMeldekort(helper, fremtidigMeldekort)

                val resultat = repo.hentAlleMeldekortKlarTilInnsending(fnr)
                resultat shouldBe emptyList()
            }
        }

        private fun lagMeldekort(
            fnr: Fnr,
            søndag: LocalDate,
            mottatt: LocalDateTime? = null,
        ) = ObjectMother.meldekort(
            fnr = fnr,
            periode = ObjectMother.periode(tilSisteSøndagEtter = søndag),
            mottatt = mottatt,
        )
    }
}
