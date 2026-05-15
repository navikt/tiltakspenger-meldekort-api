package no.nav.tiltakspenger.fakes.repos

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.bruker.BrukerSakRepo
import no.nav.tiltakspenger.meldekort.bruker.SakForBruker

/**
 * In-memory speiling av [no.nav.tiltakspenger.meldekort.bruker.infra.repo.BrukerSakPostgresRepo].
 *
 * Leser fra samme underliggende lager som [SakRepoFake] for å unngå at fake-grensesnittet
 * inneholder duplikat tilstand. Returnerer kun feltene som er definert i [SakForBruker].
 */
class BrukerSakRepoFake(
    private val sakRepoFake: SakRepoFake,
) : BrukerSakRepo {

    override fun hentForBruker(
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): SakForBruker? {
        val sak = sakRepoFake.alleSaker().firstOrNull { it.fnr == fnr } ?: return null
        return SakForBruker(
            fnr = sak.fnr,
            arenaMeldekortStatus = sak.arenaMeldekortStatus,
            harSoknadUnderBehandling = sak.harSoknadUnderBehandling,
            kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort,
        )
    }
}
