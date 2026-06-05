package no.nav.tiltakspenger.meldekort.sak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode.Companion.krevSortertEtterPeriodeOgVersjon

/**
 * Merk at denne brukes av [no.nav.tiltakspenger.meldekort.bruker.BrukerService].
 * Jo mer som legges til her, jo tregere vil bruker-aggregatet være.
 * TODO jah: Splitt ut til custom spørring og domenemodell for bruker.
 */
data class Sak(
    val id: SakId,
    val saksnummer: String,
    val fnr: Fnr,
    val meldeperioder: List<Meldeperiode>,
    val arenaMeldekortStatus: ArenaMeldekortStatus,
    val harSoknadUnderBehandling: Boolean,
    val kanSendeInnHelgForMeldekort: Boolean,
    val meldekortvedtak: List<Meldekortvedtak> = emptyList(),
) {

    val meldekortvedtakIdListe: List<VedtakId> = meldekortvedtak.map { it.id }

    init {
        meldeperioder.krevSortertEtterPeriodeOgVersjon()
    }
}
