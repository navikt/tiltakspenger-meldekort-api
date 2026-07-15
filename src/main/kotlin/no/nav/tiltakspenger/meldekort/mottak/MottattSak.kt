package no.nav.tiltakspenger.meldekort.mottak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode.Companion.krevSortertEtterPeriodeOgVersjon
import no.nav.tiltakspenger.meldekort.sak.Sak

/**
 * Skrivemodell for en sak slik vi mottok den fra saksbehandling-api.
 *
 * I motsetning til lesemodellen [Sak] inneholder denne kun feltene saksbehandling-api faktisk sender oss.
 * Leseside-felter som [Sak.arenaMeldekortStatus] (som settes/oppdateres av egne jobber i dette repoet) finnes derfor ikke her.
 * Servicen avgjør om den mottatte saken skal lagres som ny eller oppdatere en eksisterende.
 */
data class MottattSak(
    val id: SakId,
    val saksnummer: String,
    val fnr: Fnr,
    val meldeperioder: List<Meldeperiode>,
    val harSoknadUnderBehandling: Boolean,
    val kanSendeInnHelgForMeldekort: Boolean,
    val meldekortvedtak: List<Meldekortvedtak> = emptyList(),
) {

    init {
        meldeperioder.krevSortertEtterPeriodeOgVersjon()
    }

    /**
     * Sammenligner den mottatte saken med en allerede lagret [Sak].
     * Leseside-felter (f.eks. arenaMeldekortStatus) er ikke relevante for om de er like, og holdes utenfor.
     *
     * Skrivemodellen får her kjenne til lesemodellen [Sak] (men ikke omvendt) – det er en akseptert, enveis avhengighet.
     */
    fun erLik(eksisterende: Sak): Boolean =
        id == eksisterende.id &&
            saksnummer == eksisterende.saksnummer &&
            fnr == eksisterende.fnr &&
            meldeperioder == eksisterende.meldeperioder &&
            harSoknadUnderBehandling == eksisterende.harSoknadUnderBehandling &&
            kanSendeInnHelgForMeldekort == eksisterende.kanSendeInnHelgForMeldekort &&
            meldekortvedtak == eksisterende.meldekortvedtak
}
