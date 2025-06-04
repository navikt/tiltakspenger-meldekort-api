package no.nav.tiltakspenger.meldekort.service

import arrow.core.getOrElse
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.SaksbehandlingClient
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.domene.Bruker
import no.nav.tiltakspenger.meldekort.domene.Sak
import no.nav.tiltakspenger.meldekort.repository.SakRepo

class BrukerService(
    private val meldekortService: MeldekortService,
    private val sakRepo: SakRepo,
    private val arenaMeldekortStatusService: ArenaMeldekortStatusService,
    private val saksbehandlingClient: SaksbehandlingClient,
) {
    suspend fun hentBruker(fnr: Fnr): Bruker {
        val sak = sakRepo.hentTilBruker(fnr)

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
        )
    }

    private suspend fun hentBrukerUtenSak(fnr: Fnr): Bruker.UtenSak {
        val arenaMeldekortStatus = arenaMeldekortStatusService.hentArenaMeldekortStatus(fnr)
        val harSoknadUnderBehandling = if (arenaMeldekortStatus != ArenaMeldekortStatus.HAR_MELDEKORT) {
            // Defaulter til false hvis kallet til saksbehandling-api feiler siden det er viktigere at de får opp siden enn at feilmeldingen er korrekt
            saksbehandlingClient.harSoknadUnderBehandling(fnr).getOrElse { false }
        } else {
            false
        }

        return Bruker.UtenSak(
            fnr = fnr,
            arenaMeldekortStatus = arenaMeldekortStatus,
            harSoknadUnderBehandling = harSoknadUnderBehandling,
        )
    }
}
