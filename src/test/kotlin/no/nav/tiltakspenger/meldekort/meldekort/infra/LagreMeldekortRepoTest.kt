package no.nav.tiltakspenger.meldekort.meldekort.infra

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.journalføring.JournalpostId
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LagreMeldekortRepoTest {

    @Nested
    inner class Lagre {
        @Test
        fun `lagres og kan hentes ut`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = nå(fixedClock))

                lagreMeldekort(helper, meldekort)

                val result = repo.hentForMeldekortId(meldekort.id, meldekort.meldeperiode.fnr)

                result shouldNotBe null
                result!!.id shouldBe meldekort.id
                result.meldeperiode.id shouldBe meldekort.meldeperiode.id
                result.mottatt shouldBe meldekort.mottatt
            }
        }

        @Test
        fun `returnerer null for ukjent meldekortId`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = helper.nesteFnr()

                val result = repo.hentForMeldekortId(
                    no.nav.tiltakspenger.libs.common.MeldekortId.random(),
                    fnr,
                )

                result shouldBe null
            }
        }

        @Test
        fun `returnerer null for feil fnr`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = nå(fixedClock))

                lagreMeldekort(helper, meldekort)

                val result = repo.hentForMeldekortId(meldekort.id, helper.nesteFnr())

                result shouldBe null
            }
        }

        @Test
        fun `lagrer og henter meldekort med journalpostId`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val journalførtMeldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = nå(fixedClock)).copy(
                    journalpostId = JournalpostId("jp-123"),
                    journalføringstidspunkt = nå(fixedClock),
                )

                lagreMeldekort(helper, journalførtMeldekort)

                val result = repo.hentForMeldekortId(journalførtMeldekort.id, journalførtMeldekort.meldeperiode.fnr)

                result shouldBe journalførtMeldekort
            }
        }

        @Test
        fun `lagre oppdaterer eksisterende meldekort med samme id`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = null, locale = null)
                lagreMeldekort(helper, meldekort)

                val oppdatertMeldekort = meldekort.copy(
                    mottatt = nå(fixedClock),
                    korrigering = true,
                    locale = "nb",
                )

                repo.lagre(oppdatertMeldekort)

                repo.hentForMeldekortId(meldekort.id, meldekort.fnr) shouldBe oppdatertMeldekort
            }
        }

        @Test
        fun `lagrer korrigering`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = null)

                lagreMeldekort(helper, meldekort)

                val korrigering = meldekort.copy(
                    mottatt = nå(fixedClock),
                    korrigering = true,
                )

                repo.lagre(korrigering)

                repo.hentForMeldekortId(meldekort.id, meldekort.fnr).also { result ->
                    result shouldNotBe null
                    result!!.korrigering shouldBe true
                }
            }
        }
    }

    @Nested
    inner class LagreInnsendtMeldekortFraBruker {
        @Test
        fun `oppdaterer mottatt, dager og locale når meldekortet er åpent for innsending`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = null, locale = null)
                lagreMeldekort(helper, meldekort)

                val innsendt = meldekort.copy(mottatt = nå(fixedClock), locale = "nb")
                val antallOppdaterte = repo.lagreInnsendtMeldekortFraBruker(innsendt)

                antallOppdaterte shouldBe 1
                repo.hentForMeldekortId(meldekort.id, meldekort.fnr).also { result ->
                    result shouldNotBe null
                    result!!.mottatt shouldBe innsendt.mottatt
                    result.locale shouldBe "nb"
                }
            }
        }

        @Test
        fun `oppdaterer ikke et meldekort som allerede er mottatt`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val alleredeMottatt = nå(fixedClock)
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = alleredeMottatt, locale = "nb")
                lagreMeldekort(helper, meldekort)

                val nyInnsending = meldekort.copy(mottatt = nå(fixedClock).plusDays(1), locale = "en")
                val antallOppdaterte = repo.lagreInnsendtMeldekortFraBruker(nyInnsending)

                antallOppdaterte shouldBe 0
                repo.hentForMeldekortId(meldekort.id, meldekort.fnr).also { result ->
                    result shouldNotBe null
                    result!!.mottatt shouldBe alleredeMottatt
                    result.locale shouldBe "nb"
                }
            }
        }

        @Test
        fun `oppdaterer ikke et deaktivert meldekort`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = null, locale = null)
                lagreMeldekort(helper, meldekort)
                repo.deaktiver(meldekort.id)

                val innsendt = meldekort.copy(mottatt = nå(fixedClock), locale = "nb")
                val antallOppdaterte = repo.lagreInnsendtMeldekortFraBruker(innsendt)

                antallOppdaterte shouldBe 0
                repo.hentForMeldekortId(meldekort.id, meldekort.fnr).also { result ->
                    result shouldNotBe null
                    result!!.mottatt shouldBe null
                }
            }
        }

        @Test
        fun `oppdaterer ikke et ukjent meldekort`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = null)

                val antallOppdaterte = repo.lagreInnsendtMeldekortFraBruker(meldekort.copy(mottatt = nå(fixedClock)))

                antallOppdaterte shouldBe 0
            }
        }
    }

    @Nested
    inner class Deaktiver {
        @Test
        fun `deaktiver setter deaktivert`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = null)

                lagreMeldekort(helper, meldekort)
                repo.hentForMeldekortId(meldekort.id, meldekort.fnr).also { result ->
                    result shouldNotBe null
                    result!!.deaktivert shouldBe null
                }
                repo.deaktiver(meldekort.id)

                repo.hentForMeldekortId(meldekort.id, meldekort.fnr).also { result ->
                    result shouldNotBe null
                    result!!.deaktivert shouldNotBe null
                }
            }
        }

        @Test
        fun `deaktiver setter deaktivert tidspunkt`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = null)

                lagreMeldekort(helper, meldekort)
                repo.hentForMeldekortId(meldekort.id, meldekort.fnr).also { result ->
                    result shouldNotBe null
                    result!!.deaktivert shouldBe null
                }
                repo.deaktiver(meldekort.id)

                repo.hentForMeldekortId(meldekort.id, meldekort.fnr).also { result ->
                    result shouldNotBe null
                    result!!.deaktivert shouldNotBe null
                }
            }
        }

        @Test
        fun `deaktivert meldekort vises ikke i hentNesteMeldekortTilUtfylling`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr(), mottatt = null)

                lagreMeldekort(helper, meldekort)

                repo.deaktiver(meldekort.id)

                val neste = repo.hentNesteMeldekortTilUtfylling(meldekort.fnr)
                neste shouldBe null
            }
        }
    }

    @Nested
    inner class HentMeldekortForKjedeId {
        @Test
        fun `returnerer alle meldekort i riktig rekkefølge etter versjon`() {
            withMigratedDb(runIsolated = false, clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = helper.nesteFnr()
                val sakId = SakId.random()
                val saksnummer = helper.nesteSaksnummer()
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val kjedeId = MeldeperiodeKjedeId.fraPeriode(periode)

                val meldeperiode1 = ObjectMother.meldeperiode(
                    periode = periode,
                    kjedeId = kjedeId,
                    fnr = fnr,
                    sakId = sakId,
                    saksnummer = saksnummer,
                    versjon = 1,
                    opprettet = nå(fixedClock),
                )
                val meldekort1 = ObjectMother.meldekort(
                    meldeperiode = meldeperiode1,
                    fnr = fnr,
                    sakId = sakId,
                    saksnummer = saksnummer,
                    periode = periode,
                    mottatt = nå(fixedClock),
                )

                val meldeperiode2 = ObjectMother.meldeperiode(
                    periode = periode,
                    kjedeId = kjedeId,
                    fnr = fnr,
                    sakId = sakId,
                    saksnummer = saksnummer,
                    versjon = 2,
                    opprettet = nå(fixedClock).plusHours(1),
                )
                val meldekort2 = ObjectMother.meldekort(
                    meldeperiode = meldeperiode2,
                    fnr = fnr,
                    sakId = sakId,
                    saksnummer = saksnummer,
                    periode = periode,
                    mottatt = null,
                )

                lagreMeldekort(helper, meldekort1, meldekort2)

                val kjede = repo.hentMeldekortForKjedeId(kjedeId, fnr)

                kjede.size shouldBe 2
                kjede[0].id shouldBe meldekort1.id
                kjede[0].meldeperiode.versjon shouldBe 1
                kjede[1].id shouldBe meldekort2.id
                kjede[1].meldeperiode.versjon shouldBe 2
            }
        }

        @Test
        fun `returnerer tom kjede for ukjent kjedeId`() {
            withMigratedDb(runIsolated = false) { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(fnr = helper.nesteFnr())

                lagreMeldekort(helper, meldekort)

                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val ukjentKjedeId = MeldeperiodeKjedeId.fraPeriode(periode)

                val kjede = repo.hentMeldekortForKjedeId(ukjentKjedeId, meldekort.fnr)

                kjede.size shouldBe 0
            }
        }

        @Test
        fun `filtrerer på fnr`() {
            withMigratedDb(runIsolated = false, clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr1 = helper.nesteFnr()
                val fnr2 = helper.nesteFnr()
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

                val meldekort1 = ObjectMother.meldekort(
                    fnr = fnr1,
                    periode = periode,
                    mottatt = nå(fixedClock),
                )
                val meldekort2 = ObjectMother.meldekort(
                    fnr = fnr2,
                    periode = periode,
                    mottatt = nå(fixedClock),
                )

                lagreMeldekort(helper, meldekort1, meldekort2)

                val kjedeId = meldekort1.meldeperiode.kjedeId
                val kjede = repo.hentMeldekortForKjedeId(kjedeId, fnr1)

                kjede.size shouldBe 1
                kjede[0].id shouldBe meldekort1.id
            }
        }
    }
}
