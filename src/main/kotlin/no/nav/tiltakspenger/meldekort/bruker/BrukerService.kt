package no.nav.tiltakspenger.meldekort.bruker

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatusService
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortService
import no.nav.tiltakspenger.meldekort.sak.Sak
import no.nav.tiltakspenger.meldekort.sak.SakRepo

class BrukerService(
    private val meldekortService: MeldekortService,
    private val sakRepo: SakRepo,
    private val arenaMeldekortStatusService: ArenaMeldekortStatusService,
) {
    suspend fun hentBruker(fnr: Fnr): Bruker {
        // sak uten meldeperioder og meldekortvedtak.
        val sak = sakRepo.hentForBruker(fnr)

        return if (sak != null) {
            hentBrukerMedSak(sak)
        } else {
            hentBrukerUtenSak(fnr)
        }
    }

    private fun hentBrukerMedSak(sak: Sak): Bruker.MedSak {
        val nesteMeldekort = meldekortService.hentNesteMeldekortForUtfylling(sak.fnr)
        val sisteMeldekort = meldekortService.hentSisteUtfylteMeldekort(sak.fnr)

        return Bruker.MedSak(
            sak = sak,
            nesteMeldekort = nesteMeldekort,
            sisteMeldekort = sisteMeldekort,
            harSoknadUnderBehandling = sak.harSoknadUnderBehandling,
            kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort,
        )
    }

    private suspend fun hentBrukerUtenSak(fnr: Fnr): Bruker.UtenSak {
        val arenaMeldekortStatus = arenaMeldekortStatusService.hentArenaMeldekortStatus(fnr)

        return Bruker.UtenSak(
            fnr = fnr,
            arenaMeldekortStatus = arenaMeldekortStatus,
        )
    }
}
