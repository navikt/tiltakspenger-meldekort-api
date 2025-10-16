package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr

sealed interface Bruker {
    data class MedSak(
        val sak: Sak,
        val nesteMeldekort: Meldekort?,
        val sisteMeldekort: Meldekort?,
        val harSoknadUnderBehandling: Boolean,
        val kanSendeInnHelgForMeldekort: Boolean,
    ) : Bruker

    data class UtenSak(
        val fnr: Fnr,
        val arenaMeldekortStatus: ArenaMeldekortStatus,
    ) : Bruker
}
