package no.nav.tiltakspenger.meldekort.sak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode

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

    fun erLik(otherSak: Sak): Boolean {
        // Enkelte felter er ikke relevante for å avgjøre om to saker er like, dermed kopierer vi disse feltene før sammenligningen
        return this.copy(
            arenaMeldekortStatus = otherSak.arenaMeldekortStatus,
        ) == otherSak
    }

    init {
        meldeperioder.zipWithNext().forEach { (a, b) ->
            require(a.periode.tilOgMed < b.periode.fraOgMed || (a.periode == b.periode && a.versjon < b.versjon)) {
                "Meldeperioder må være sortert etter periode og versjon. Fikk $a før $b"
            }
        }
    }
}

enum class ArenaMeldekortStatus {
    UKJENT,
    HAR_MELDEKORT,
    HAR_IKKE_MELDEKORT,
}
