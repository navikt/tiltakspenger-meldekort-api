package no.nav.tiltakspenger.meldekort.meldekort.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortMedSisteMeldeperiode
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortStatus
import no.nav.tiltakspenger.meldekort.meldekortvedtak.infra.lagreMeldekortvedtak
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
            withMigratedDb(runIsolated = false, clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = helper.nesteFnr()
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val førsteMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = førstePeriode.tilOgMed.atTime(0, 0),
                    periode = førstePeriode,
                )
                val andreMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
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
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = null)

                lagreMeldekort(helper, meldekort)

                val result = repo.hentSisteUtfylteMeldekort(meldekort.fnr)
                result shouldBe null
            }
        }

        @Test
        fun `returnerer nyeste mottatte meldekort når flere er mottatt`() {
            withMigratedDb(runIsolated = false, clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = helper.nesteFnr()
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val førsteMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = nå(fixedClock).minusWeeks(2),
                    periode = førstePeriode,
                )
                val andreMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
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

        @Test
        fun `filtrerer siste mottatte meldekort på fnr`() {
            withMigratedDb(runIsolated = false, clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = helper.nesteFnr()
                val annenFnr = helper.nesteFnr()
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val brukersMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = nå(fixedClock).minusWeeks(2),
                    periode = førstePeriode,
                )
                val annenBrukersNyereMeldekort = ObjectMother.meldekort(
                    fnr = annenFnr,
                    mottatt = nå(fixedClock),
                    periode = førstePeriode.plus14Dager(),
                )

                lagreMeldekort(helper, brukersMeldekort, annenBrukersNyereMeldekort)

                repo.hentSisteUtfylteMeldekort(fnr) shouldBe brukersMeldekort
            }
        }
    }

    @Nested
    inner class HentNesteMeldekortTilUtfylling {
        @Test
        fun `skal hente meldekort som kan utfylles og forrige innsendte meldekort`() {
            withMigratedDb(runIsolated = false, clock = fixedClockAt(1.mars(2025))) { helper ->
                val fnr = helper.nesteFnr()
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val førsteMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = førstePeriode.tilOgMed.atTime(0, 0),
                    periode = førstePeriode,
                )
                val andreMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    periode = førstePeriode.plus14Dager(),
                )

                lagreMeldekort(helper, førsteMeldekort, andreMeldekort)

                val repo = helper.meldekortPostgresRepo

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
            withMigratedDb(runIsolated = false) { helper ->
                val fnr = helper.nesteFnr()
                val førstePeriode = ObjectMother.periode(LocalDate.now().minusWeeks(2))

                val førsteMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    periode = førstePeriode,
                )
                val andreMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    periode = førstePeriode.plus14Dager(),
                )

                lagreMeldekort(helper, førsteMeldekort, andreMeldekort)

                val repo = helper.meldekortPostgresRepo

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
            withMigratedDb(runIsolated = false) { helper ->
                val fnr = helper.nesteFnr()
                val førstePeriode = ObjectMother.periode(LocalDate.now())

                val førsteMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    periode = førstePeriode,
                )
                val andreMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
                    ),
                )

                lagreMeldekort(helper, førsteMeldekort, andreMeldekort)

                val repo = helper.meldekortPostgresRepo

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
            withMigratedDb(runIsolated = false, clock = fixedClockAt(1.mars(2025))) { helper ->
                val fnr = helper.nesteFnr()
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val førsteMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = nå(fixedClock).minusWeeks(2),
                    periode = førstePeriode,
                )
                val andreMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = nå(fixedClock),
                    periode = Periode(
                        fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                        tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
                    ),
                )

                lagreMeldekort(helper, førsteMeldekort, andreMeldekort)

                val repo = helper.meldekortPostgresRepo

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
            withMigratedDb(runIsolated = false) { helper ->
                val fnr = helper.nesteFnr()
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val førsteMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    periode = førstePeriode,
                )
                val andreMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
                    mottatt = null,
                    periode = førstePeriode.plus14Dager(),
                )

                lagreMeldekort(helper, førsteMeldekort, andreMeldekort)

                val repo = helper.meldekortPostgresRepo

                val sisteMeldekortFraDb = repo.hentSisteUtfylteMeldekort(fnr)
                val nesteMeldekortFraDb = repo.hentNesteMeldekortTilUtfylling(fnr)
                val alleInnsendteMeldekort = repo.hentInnsendteMeldekortForBruker(fnr)

                sisteMeldekortFraDb shouldBe null
                nesteMeldekortFraDb shouldBe førsteMeldekort
                alleInnsendteMeldekort shouldBe emptyList()
            }
        }

        @Test
        fun `skal hente nyeste versjon når flere meldekort for samme periode kan utfylles`() {
            withMigratedDb(runIsolated = false, clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = helper.nesteFnr()
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val meldekortVersjon1 = ObjectMother.meldekort(
                    fnr = fnr,
                    periode = periode,
                    mottatt = null,
                )
                val meldeperiodeVersjon2 = ObjectMother.meldeperiode(
                    periode = periode,
                    fnr = fnr,
                    sakId = meldekortVersjon1.sakId,
                    saksnummer = meldekortVersjon1.saksnummer,
                    versjon = 2,
                    opprettet = meldekortVersjon1.meldeperiode.opprettet.plusSeconds(1),
                )
                val meldekortVersjon2 = ObjectMother.meldekort(
                    fnr = fnr,
                    sakId = meldekortVersjon1.sakId,
                    saksnummer = meldekortVersjon1.saksnummer,
                    periode = periode,
                    meldeperiode = meldeperiodeVersjon2,
                    mottatt = null,
                )

                lagreMeldekort(helper, meldekortVersjon1, meldekortVersjon2)

                repo.hentNesteMeldekortTilUtfylling(fnr) shouldBe meldekortVersjon2
            }
        }

        @Test
        fun `filtrerer neste meldekort til utfylling på fnr`() {
            withMigratedDb(runIsolated = false, clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = helper.nesteFnr()
                val annenFnr = helper.nesteFnr()
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val brukersMeldekort = ObjectMother.meldekort(fnr = fnr, periode = førstePeriode.plus14Dager(), mottatt = null)
                val annenBrukersTidligereMeldekort = ObjectMother.meldekort(fnr = annenFnr, periode = førstePeriode, mottatt = null)

                lagreMeldekort(helper, brukersMeldekort, annenBrukersTidligereMeldekort)

                repo.hentNesteMeldekortTilUtfylling(fnr) shouldBe brukersMeldekort
            }
        }

        @Test
        fun `returnerer ikke meldekort når kjeden har et meldekortvedtak (papirmeldekort)`() {
            withMigratedDb(runIsolated = false, clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = null, periode = periode)

                lagreMeldekort(helper, meldekort)
                lagreMeldekortvedtak(helper, ObjectMother.meldekortvedtak(meldekort = meldekort))

                repo.hentNesteMeldekortTilUtfylling(meldekort.fnr) shouldBe null
            }
        }

        @Test
        fun `hopper over kjede med meldekortvedtak og returnerer neste kjede uten vedtak`() {
            withMigratedDb(runIsolated = false, clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = helper.nesteFnr()
                val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val førsteMeldekort = ObjectMother.meldekort(fnr = fnr, mottatt = null, periode = førstePeriode)
                val andreMeldekort = ObjectMother.meldekort(
                    fnr = fnr,
                    sakId = førsteMeldekort.sakId,
                    saksnummer = førsteMeldekort.saksnummer,
                    mottatt = null,
                    periode = førstePeriode.plus14Dager(),
                )

                lagreMeldekort(helper, førsteMeldekort, andreMeldekort)

                // Bruker har "papirmeldekort" (meldekortvedtak) for første kjede
                lagreMeldekortvedtak(helper, ObjectMother.meldekortvedtak(meldekort = førsteMeldekort))

                repo.hentNesteMeldekortTilUtfylling(fnr) shouldBe andreMeldekort
            }
        }
    }

    @Nested
    inner class HentInnsendteMeldekortForBruker {
        @Test
        fun `skal hente nyeste meldeperiode for innsendt meldekort`() {
            withMigratedDb(runIsolated = false, clock = fixedClockAt(1.mars(2025))) { helper ->
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val meldekort = ObjectMother.meldekort(
                    fnr = helper.nesteFnr(),
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
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = null)

                lagreMeldekort(helper, meldekort)

                val result = repo.hentInnsendteMeldekortForBruker(meldekort.fnr)
                result shouldBe emptyList()
            }
        }

        @Test
        fun `returnerer journalpostId for journalført innsendt meldekort`() {
            withMigratedDb(runIsolated = false, clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val meldekort = ObjectMother.meldekort(
                    fnr = helper.nesteFnr(),
                    mottatt = nå(fixedClock),
                    periode = periode,
                )
                val journalpostId = no.nav.tiltakspenger.meldekort.journalføring.JournalpostId("jp-999")
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
            withMigratedDb(runIsolated = false, clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val meldekort = ObjectMother.meldekort(
                    fnr = helper.nesteFnr(),
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
            withMigratedDb(runIsolated = false) { helper ->
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr())
                val kjedeId = meldekort.meldeperiode.kjedeId
                lagreMeldekort(helper, meldekort)
                val hentetMeldekort = helper.meldekortPostgresRepo.hentSisteMeldekortForKjedeId(kjedeId, meldekort.fnr)
                hentetMeldekort shouldBe meldekort
            }
        }

        @Test
        fun `returnerer null for ukjent kjedeId`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = helper.nesteFnr()
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val ukjentKjedeId = no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId.fraPeriode(periode)

                val result = repo.hentSisteMeldekortForKjedeId(ukjentKjedeId, fnr)
                result shouldBe null
            }
        }

        @Test
        fun `returnerer siste meldekortversjon for kjede`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val meldekortVersjon1 = ObjectMother.meldekort(fnr = helper.nesteFnr(), periode = periode, mottatt = null)
                val meldeperiodeVersjon2 = ObjectMother.meldeperiode(
                    periode = periode,
                    fnr = meldekortVersjon1.fnr,
                    sakId = meldekortVersjon1.sakId,
                    saksnummer = meldekortVersjon1.saksnummer,
                    versjon = 2,
                    opprettet = meldekortVersjon1.meldeperiode.opprettet.plusSeconds(1),
                )
                val meldekortVersjon2 = ObjectMother.meldekort(
                    periode = periode,
                    fnr = meldekortVersjon1.fnr,
                    sakId = meldekortVersjon1.sakId,
                    saksnummer = meldekortVersjon1.saksnummer,
                    meldeperiode = meldeperiodeVersjon2,
                    mottatt = null,
                )

                lagreMeldekort(helper, meldekortVersjon1, meldekortVersjon2)

                repo.hentSisteMeldekortForKjedeId(meldekortVersjon1.meldeperiode.kjedeId, meldekortVersjon1.fnr) shouldBe meldekortVersjon2
            }
        }

        @Test
        fun `returnerer null for feil fnr`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr())
                lagreMeldekort(helper, meldekort)

                repo.hentSisteMeldekortForKjedeId(meldekort.meldeperiode.kjedeId, helper.nesteFnr()) shouldBe null
            }
        }
    }

    @Nested
    inner class HentAlleMeldekortKlarTilInnsending {
        @Test
        fun `henter alle meldekort bruker kan fylle ut`() {
            val clock = fixedClockAt(1.mars(2025))
            withMigratedDb(runIsolated = false, clock = clock) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = helper.nesteFnr()
                val nærmesteSøndag = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

                val nærmesteMeldekort = lagMeldekort(fnr, nærmesteSøndag)
                val forrigeForrigeMeldekort = lagMeldekort(fnr, nærmesteSøndag.minusWeeks(4))
                val forrigeMeldekort = lagMeldekort(fnr, nærmesteSøndag.minusWeeks(2))
                val nesteMeldekort = lagMeldekort(fnr, nærmesteSøndag.plusWeeks(2))
                val innsendtMeldekort = ObjectMother.meldekort(
                    meldeperiode = forrigeMeldekort.meldeperiode,
                    mottatt = nå(fixedClockAt(nærmesteSøndag.minusWeeks(2))),
                )
                val annenBrukersMeldekort = lagMeldekort(helper.nesteFnr(), nærmesteSøndag)

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
            withMigratedDb(runIsolated = false, clock = clock) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = helper.nesteFnr()
                val fremtidigSøndag = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).plusWeeks(4)

                val fremtidigMeldekort = lagMeldekort(fnr, fremtidigSøndag)
                lagreMeldekort(helper, fremtidigMeldekort)

                val resultat = repo.hentAlleMeldekortKlarTilInnsending(fnr)
                resultat shouldBe emptyList()
            }
        }

        @Test
        fun `returnerer ikke deaktiverte meldekort som ellers er klare til innsending`() {
            val clock = fixedClockAt(1.mars(2025))
            withMigratedDb(runIsolated = false, clock = clock) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = helper.nesteFnr()
                val nærmesteSøndag = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                val meldekort = lagMeldekort(fnr, nærmesteSøndag)
                lagreMeldekort(helper, meldekort)

                repo.deaktiver(meldekort.id)

                repo.hentAlleMeldekortKlarTilInnsending(fnr) shouldBe emptyList()
            }
        }

        @Test
        fun `returnerer ikke meldekort for kjede som har et meldekortvedtak (papirmeldekort)`() {
            val clock = fixedClockAt(1.mars(2025))
            withMigratedDb(runIsolated = false, clock = clock) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = helper.nesteFnr()
                val nærmesteSøndag = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

                val medVedtak = lagMeldekort(fnr, nærmesteSøndag.minusWeeks(2))
                val utenVedtak = lagMeldekort(fnr, nærmesteSøndag)

                lagreMeldekort(helper, medVedtak, utenVedtak)
                lagreMeldekortvedtak(helper, ObjectMother.meldekortvedtak(meldekort = medVedtak))

                val resultat = repo.hentAlleMeldekortKlarTilInnsending(fnr)

                resultat.map { it.id } shouldBe listOf(utenVedtak.id)
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
