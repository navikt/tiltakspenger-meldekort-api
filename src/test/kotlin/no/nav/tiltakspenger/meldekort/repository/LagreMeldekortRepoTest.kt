package no.nav.tiltakspenger.meldekort.repository

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LagreMeldekortRepoTest {

    @Nested
    inner class Lagre {
        @Test
        fun `lagres og kan hentes ut`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(mottatt = nå(fixedClock))

                lagreMeldekort(helper, meldekort)

                val result = repo.hentForMeldekortId(meldekort.id, meldekort.meldeperiode.fnr)

                result shouldNotBe null
                result!!.id shouldBe meldekort.id
                result.meldeperiode.id shouldBe meldekort.meldeperiode.id
                result.mottatt shouldBe meldekort.mottatt
                result.varselId shouldBe meldekort.varselId
            }
        }

        @Test
        fun `returnerer null for ukjent meldekortId`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = Fnr.random()

                val result = repo.hentForMeldekortId(
                    no.nav.tiltakspenger.libs.common.MeldekortId.random(),
                    fnr,
                )

                result shouldBe null
            }
        }

        @Test
        fun `returnerer null for feil fnr`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(mottatt = nå(fixedClock))

                lagreMeldekort(helper, meldekort)

                val result = repo.hentForMeldekortId(meldekort.id, Fnr.random())

                result shouldBe null
            }
        }

        @Test
        fun `lagrer og henter meldekort med journalpostId`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val journalførtMeldekort = ObjectMother.meldekort(mottatt = nå(fixedClock)).copy(
                    journalpostId = JournalpostId("jp-123"),
                    journalføringstidspunkt = nå(fixedClock),
                )

                lagreMeldekort(helper, journalførtMeldekort)

                val result = repo.hentForMeldekortId(journalførtMeldekort.id, journalførtMeldekort.meldeperiode.fnr)

                result shouldBe journalførtMeldekort
            }
        }
    }

    @Nested
    inner class Deaktiver {
        @Test
        fun `deaktiver med deaktiverVarsel true setter deaktivert og varsel_inaktivert til false`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(mottatt = null)

                lagreMeldekort(helper, meldekort)
                repo.hentForMeldekortId(meldekort.id, meldekort.fnr).also { result ->
                    result shouldNotBe null
                    result!!.deaktivert shouldBe null
                    result.erVarselInaktivert shouldBe false
                    result.sendtVarselTidspunkt shouldBe null
                    result.sendtVarsel shouldBe false
                }
                repo.deaktiver(meldekort.id, deaktiverVarsel = true)

                repo.hentForMeldekortId(meldekort.id, meldekort.fnr).also { result ->
                    result shouldNotBe null
                    result!!.deaktivert shouldNotBe null
                    result.erVarselInaktivert shouldBe false
                    result.sendtVarselTidspunkt shouldBe null
                    result.sendtVarsel shouldBe false
                }
            }
        }

        @Test
        fun `deaktiver med deaktiverVarsel false endrer erVarselInaktivert til true`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(mottatt = null)

                lagreMeldekort(helper, meldekort)
                repo.hentForMeldekortId(meldekort.id, meldekort.fnr).also { result ->
                    result shouldNotBe null
                    result!!.deaktivert shouldBe null
                    result.erVarselInaktivert shouldBe false
                    result.sendtVarselTidspunkt shouldBe null
                    result.sendtVarsel shouldBe false
                }
                repo.deaktiver(meldekort.id, deaktiverVarsel = false)

                repo.hentForMeldekortId(meldekort.id, meldekort.fnr).also { result ->
                    result shouldNotBe null
                    result!!.deaktivert shouldNotBe null
                    // I praksis vil varselet aldri innaktiveres fra systemet.
                    result.erVarselInaktivert shouldBe true
                    result.sendtVarselTidspunkt shouldBe null
                    result.sendtVarsel shouldBe false
                }
            }
        }

        @Test
        fun `deaktivert meldekort vises ikke i hentNesteMeldekortTilUtfylling`() {
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort(mottatt = null)

                lagreMeldekort(helper, meldekort)

                repo.deaktiver(meldekort.id, deaktiverVarsel = false)

                val neste = repo.hentNesteMeldekortTilUtfylling(meldekort.fnr)
                neste shouldBe null
            }
        }
    }

    @Nested
    inner class HentMeldekortForKjedeId {
        @Test
        fun `returnerer alle meldekort i riktig rekkefølge etter versjon`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr = Fnr.random()
                val sakId = SakId.random()
                val saksnummer = "SAK-123"
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

                helper.meldeperiodeRepo.lagre(meldeperiode1)
                helper.meldekortPostgresRepo.lagre(meldekort1)
                helper.meldeperiodeRepo.lagre(meldeperiode2)
                helper.meldekortPostgresRepo.lagre(meldekort2)

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
            withMigratedDb { helper ->
                val repo = helper.meldekortPostgresRepo
                val meldekort = ObjectMother.meldekort()

                lagreMeldekort(helper, meldekort)

                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val ukjentKjedeId = MeldeperiodeKjedeId.fraPeriode(periode)

                val kjede = repo.hentMeldekortForKjedeId(ukjentKjedeId, meldekort.fnr)

                kjede.size shouldBe 0
            }
        }

        @Test
        fun `filtrerer på fnr`() {
            withMigratedDb(clock = fixedClockAt(1.mars(2025))) { helper ->
                val repo = helper.meldekortPostgresRepo
                val fnr1 = Fnr.random()
                val fnr2 = Fnr.random()
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
