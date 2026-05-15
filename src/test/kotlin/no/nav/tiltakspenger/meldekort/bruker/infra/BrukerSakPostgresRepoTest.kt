package no.nav.tiltakspenger.meldekort.bruker.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.meldekort.bruker.SakForBruker
import no.nav.tiltakspenger.meldekort.sak.ArenaMeldekortStatus
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class BrukerSakPostgresRepoTest {

    @Test
    fun `hentForBruker returnerer SakForBruker med bruker-relevante felter`() {
        withMigratedDb { helper ->
            val sak = ObjectMother.sak(
                fnr = Fnr.random(),
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
        withMigratedDb { helper ->
            val sak = ObjectMother.sak(
                fnr = Fnr.random(),
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
        withMigratedDb { helper ->
            helper.brukerSakPostgresRepo.hentForBruker(Fnr.random()) shouldBe null
        }
    }

    @Test
    fun `hentForBruker følger fnr etter oppdaterFnr`() {
        withMigratedDb { helper ->
            val gammeltFnr = Fnr.random()
            val nyttFnr = Fnr.random()
            val sak = ObjectMother.sak(fnr = gammeltFnr)
            helper.sakPostgresRepo.lagre(sak)

            helper.sakPostgresRepo.oppdaterFnr(gammeltFnr, nyttFnr)

            helper.brukerSakPostgresRepo.hentForBruker(gammeltFnr) shouldBe null
            helper.brukerSakPostgresRepo.hentForBruker(nyttFnr)?.fnr shouldBe nyttFnr
        }
    }
}
