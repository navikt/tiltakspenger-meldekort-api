package no.nav.tiltakspenger.meldekort.bruker

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.sak.ArenaMeldekortStatus

sealed interface Bruker {
    data class MedSak(
        val sak: SakForBruker,
        val nesteMeldekort: BrukersMeldekort?,
        val sisteMeldekort: BrukersMeldekort?,
        val harSoknadUnderBehandling: Boolean,
        val kanSendeInnHelgForMeldekort: Boolean,
    ) : Bruker

    data class UtenSak(
        val fnr: Fnr,
        val arenaMeldekortStatus: ArenaMeldekortStatus,
    ) : Bruker
}
