package no.nav.tiltakspenger.meldekort.bruker

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatusService
import no.nav.tiltakspenger.meldekort.meldekort.HentMeldekortService

class BrukerService(
    private val hentMeldekortService: HentMeldekortService,
    private val brukerSakRepo: BrukerSakRepo,
    private val arenaMeldekortStatusService: ArenaMeldekortStatusService,
) {
    suspend fun hentBruker(fnr: Fnr): Bruker {
        val sak = brukerSakRepo.hentForBruker(fnr)

        return if (sak != null) {
            hentBrukerMedSak(sak)
        } else {
            hentBrukerUtenSak(fnr)
        }
    }

    private fun hentBrukerMedSak(sak: SakForBruker): Bruker.MedSak {
        val nesteMeldekort = hentMeldekortService.hentNesteMeldekortForUtfylling(sak.fnr)
        val sisteMeldekort = hentMeldekortService.hentSisteUtfylteMeldekort(sak.fnr)

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
