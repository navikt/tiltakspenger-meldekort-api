package no.nav.tiltakspenger.meldekort.bruker.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.bruker.SakForBruker
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class BrukerSakPostgresRepoTest {

    @Test
    fun `hentForBruker returnerer SakForBruker med bruker-relevante felter`() {
        withMigratedDb(runIsolated = false) { helper ->
            val sak = ObjectMother.sak(
                fnr = helper.nesteFnr(),
                arenaMeldekortStatus = ArenaMeldekortStatus.HAR_MELDEKORT,
                harSoknadUnderBehandling = true,
                kanSendeInnHelgForMeldekort = true,
            )
            helper.sakPostgresRepo.lagre(sak)

            helper.brukerSakPostgresRepo.hentForBruker(sak.fnr) shouldBe SakForBruker(
                fnr = sak.fnr,
                arenaMeldekortStatus = ArenaMeldekortStatus.HAR_MELDEKORT,
                harSoknadUnderBehandling = true,
                kanSendeInnHelgForMeldekort = true,
            )
        }
    }

    @Test
    fun `hentForBruker reflekterer endringer fra oppdaterArenaStatus`() {
        withMigratedDb(runIsolated = false) { helper ->
            val sak = ObjectMother.sak(
                fnr = helper.nesteFnr(),
                arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
            )
            helper.sakPostgresRepo.lagre(sak)
            helper.sakPostgresRepo.oppdaterArenaStatus(sak.id, ArenaMeldekortStatus.HAR_IKKE_MELDEKORT)

            helper.brukerSakPostgresRepo.hentForBruker(sak.fnr)?.arenaMeldekortStatus shouldBe
                ArenaMeldekortStatus.HAR_IKKE_MELDEKORT
        }
    }

    @Test
    fun `hentForBruker returnerer null for ukjent fnr`() {
        withMigratedDb(runIsolated = false) { helper ->
            helper.brukerSakPostgresRepo.hentForBruker(helper.nesteFnr()) shouldBe null
        }
    }

    @Test
    fun `hentForBruker følger fnr etter oppdaterFnr`() {
        withMigratedDb(runIsolated = false) { helper ->
            val gammeltFnr = helper.nesteFnr()
            val nyttFnr = helper.nesteFnr()
            val sak = ObjectMother.sak(fnr = gammeltFnr)
            helper.sakPostgresRepo.lagre(sak)

            helper.sakPostgresRepo.oppdaterFnr(gammeltFnr, nyttFnr)

            helper.brukerSakPostgresRepo.hentForBruker(gammeltFnr) shouldBe null
            helper.brukerSakPostgresRepo.hentForBruker(nyttFnr)?.fnr shouldBe nyttFnr
        }
    }
}
