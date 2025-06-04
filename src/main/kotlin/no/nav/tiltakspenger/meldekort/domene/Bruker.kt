package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr

sealed interface Bruker {
    data class MedSak(
        val sak: Sak,
        val nesteMeldekort: Meldekort?,
        val sisteMeldekort: Meldekort?,
    ) : Bruker

    data class UtenSak(
        val fnr: Fnr,
        val arenaMeldekortStatus: ArenaMeldekortStatus,
        val harSoknadUnderBehandling: Boolean,
    ) : Bruker
}
