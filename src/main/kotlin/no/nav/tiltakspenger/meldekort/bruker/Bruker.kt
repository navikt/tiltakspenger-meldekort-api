package no.nav.tiltakspenger.meldekort.bruker

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.meldekort.Meldekort
import no.nav.tiltakspenger.meldekort.sak.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.sak.Sak

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
