package no.nav.tiltakspenger.meldekort.bruker

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.sak.ArenaMeldekortStatus

/**
 * Read-modell for sak-data som bruker-pakka trenger.
 *
 * Inneholder bevisst kun feltene som faktisk konsumeres av brukerflyten — vi unngår å dele
 * `Sak` (det fulle aggregatet) for å forhindre antagelser om relaterte data
 * (f.eks. at `meldeperioder = emptyList()` betyr "ingen meldeperioder" vs. "ikke hentet").
 */
data class SakForBruker(
    val fnr: Fnr,
    val arenaMeldekortStatus: ArenaMeldekortStatus,
    val harSoknadUnderBehandling: Boolean,
    val kanSendeInnHelgForMeldekort: Boolean,
)
