package no.nav.tiltakspenger.fakes.repos

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideMeldekort
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideRepo
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideSak

class LandingssideRepoFake(
    private val sakRepoFake: SakRepoFake,
    private val meldekortRepoFake: MeldekortRepoFake,
    private val meldekortvedtakRepoFake: MeldekortvedtakRepoFake,
) : LandingssideRepo {

    override fun hentSak(
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): LandingssideSak? {
        val sak = sakRepoFake.alleSaker().firstOrNull { it.fnr == fnr } ?: return null
        return LandingssideSak(
            fnr = sak.fnr,
            arenaMeldekortStatus = sak.arenaMeldekortStatus,
            // Speiler LandingssidePostgresRepo: et meldekortvedtak (inkl. papirmeldekort uten
            // meldekort_bruker-rad) teller som innsendt på lik linje med innsendte meldekort_bruker.
            harInnsendteMeldekort = meldekortRepoFake.hentSisteUtfylteMeldekort(fnr) != null ||
                meldekortvedtakRepoFake.hentForSakId(sak.id).isNotEmpty(),
            meldekortTilUtfylling = meldekortRepoFake.hentAlleMeldekortKlarTilInnsending(fnr)
                .map { LandingssideMeldekort(kanSendesFra = it.meldeperiode.kanFyllesUtFraOgMed) }
                // Eksplisitt sortering på kanSendesFra for å matche sorteringsinvarianten i LandingssideSak.
                .sortedBy { it.kanSendesFra },
        )
    }
}
