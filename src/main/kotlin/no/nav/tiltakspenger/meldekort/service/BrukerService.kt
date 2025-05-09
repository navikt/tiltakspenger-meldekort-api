package no.nav.tiltakspenger.meldekort.service

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.domene.Bruker
import no.nav.tiltakspenger.meldekort.domene.Sak

class BrukerService(
    private val meldekortService: MeldekortService,
    private val sakService: SakService,
    private val arenaMeldekortStatusService: ArenaMeldekortStatusService,
) {
    suspend fun hentBruker(fnr: Fnr): Bruker {
        val sak = sakService.hentSak(fnr)

        return if (sak != null) {
            hentBrukerMedSak(sak)
        } else {
            hentBrukerUtenSak(fnr)
        }
    }

    private fun hentBrukerMedSak(sak: Sak): Bruker.MedSak {
        val nesteMeldekort = meldekortService.hentNesteMeldekortForUtfylling(sak.fnr)
        val sisteMeldekort = meldekortService.hentSisteMeldekort(sak.fnr)

        return Bruker.MedSak(
            sak = sak,
            nesteMeldekort = nesteMeldekort,
            sisteMeldekort = sisteMeldekort,
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
