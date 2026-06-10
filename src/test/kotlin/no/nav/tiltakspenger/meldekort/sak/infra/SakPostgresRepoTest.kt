package no.nav.tiltakspenger.meldekort.sak.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.lagreMeldeperiode
import no.nav.tiltakspenger.lagreSak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.sak.Sak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.oppdaterSak
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SakPostgresRepoTest {
    private fun lagreSak(helper: TestDataHelper, vararg saker: Sak) {
        saker.forEach {
            helper.lagreSak(it)
            it.meldeperioder.forEach { mp -> helper.lagreMeldeperiode(mp) }
        }
    }

    @Nested
    inner class GrunnleggendeSpørringer {

        @Test
        fun `lagrer og henter sak med meldeperioder`() {
            withMigratedDb { helper ->
                val meldeperiode1 = ObjectMother.meldeperiode(opprettet = nå(fixedClock))
                val meldeperiode2 = ObjectMother.meldeperiode(
                    periode = meldeperiode1.periode.plus14Dager(),
                    fnr = meldeperiode1.fnr,
                    sakId = meldeperiode1.sakId,
                    saksnummer = meldeperiode1.saksnummer,
                    opprettet = nå(fixedClock).plusDays(14),
                )
                val sak = ObjectMother.sak(
                    id = meldeperiode1.sakId,
                    fnr = meldeperiode1.fnr,
                    saksnummer = meldeperiode1.saksnummer,
                    meldeperioder = listOf(meldeperiode1, meldeperiode2),
                    arenaMeldekortStatus = ArenaMeldekortStatus.HAR_MELDEKORT,
                    harSoknadUnderBehandling = true,
                    kanSendeInnHelgForMeldekort = true,
                )

                lagreSak(helper, sak)

                helper.sakPostgresRepo.hent(sak.id) shouldBe sak
            }
        }

        @Test
        fun `hent returnerer null for ukjent sakId`() {
            withMigratedDb { helper ->
                helper.sakPostgresRepo.hent(SakId.random()) shouldBe null
            }
        }
    }

    @Nested
    inner class Oppdateringer {

        @Test
        fun `oppdater endrer fnr og saksbehandlingsflagg men beholder arena status`() {
            withMigratedDb { helper ->
                val opprinneligSak = ObjectMother.sak(
                    fnr = Fnr.random(),
                    arenaMeldekortStatus = ArenaMeldekortStatus.HAR_MELDEKORT,
                    harSoknadUnderBehandling = false,
                    kanSendeInnHelgForMeldekort = false,
                )
                helper.lagreSak(opprinneligSak)

                val oppdatertSak = opprinneligSak.copy(
                    fnr = Fnr.random(),
                    arenaMeldekortStatus = ArenaMeldekortStatus.HAR_IKKE_MELDEKORT,
                    harSoknadUnderBehandling = true,
                    kanSendeInnHelgForMeldekort = true,
                )
                helper.oppdaterSak(oppdatertSak)

                helper.sakPostgresRepo.hent(opprinneligSak.id) shouldBe oppdatertSak.copy(
                    arenaMeldekortStatus = opprinneligSak.arenaMeldekortStatus,
                )
            }
        }

        @Test
        fun `oppdaterFnr endrer fnr for alle saker med gammelt fnr`() {
            withMigratedDb { helper ->
                val gammeltFnr = Fnr.random()
                val nyttFnr = Fnr.random()
                val sak = ObjectMother.sak(fnr = gammeltFnr)
                helper.lagreSak(sak)

                helper.sakPostgresRepo.oppdaterFnr(gammeltFnr, nyttFnr)

                helper.sakPostgresRepo.hent(sak.id) shouldBe sak.copy(fnr = nyttFnr)
            }
        }

        @Test
        fun `oppdaterArenaStatus endrer bare arena status`() {
            withMigratedDb { helper ->
                val sak = ObjectMother.sak(
                    fnr = Fnr.random(),
                    arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
                    harSoknadUnderBehandling = true,
                    kanSendeInnHelgForMeldekort = true,
                )
                helper.lagreSak(sak)

                helper.sakPostgresRepo.oppdaterArenaStatus(sak.id, ArenaMeldekortStatus.HAR_IKKE_MELDEKORT)

                helper.sakPostgresRepo.hent(sak.id) shouldBe sak.copy(
                    arenaMeldekortStatus = ArenaMeldekortStatus.HAR_IKKE_MELDEKORT,
                )
            }
        }
    }

    @Nested
    inner class HentSakerUtenArenaStatus {

        @Test
        fun `returnerer bare saker med ukjent arena status`() {
            withMigratedDb { helper ->
                val sakMedUkjentStatus = ObjectMother.sak(fnr = Fnr.random(), arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT)
                val sakMedMeldekort = ObjectMother.sak(fnr = Fnr.random(), arenaMeldekortStatus = ArenaMeldekortStatus.HAR_MELDEKORT)
                val sakUtenMeldekort = ObjectMother.sak(fnr = Fnr.random(), arenaMeldekortStatus = ArenaMeldekortStatus.HAR_IKKE_MELDEKORT)
                lagreSak(helper, sakMedUkjentStatus, sakMedMeldekort, sakUtenMeldekort)

                helper.sakPostgresRepo.hentSakerUtenArenaStatus() shouldBe listOf(sakMedUkjentStatus.copy(meldeperioder = emptyList()))
            }
        }
    }
}
