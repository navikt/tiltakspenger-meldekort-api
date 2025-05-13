package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode

sealed interface Bruker {
    data class MedSak(
        val sak: Sak,
        val nesteMeldekort: Meldekort?,
        val sisteMeldekort: Meldekort?,
    ) : Bruker {

        fun nesteMeldeperiode(): Periode? {
            if (nesteMeldekort != null) {
                return sak.meldeperioder.find { it.fraOgMed > nesteMeldekort.periode.tilOgMed }
            }
            if (sisteMeldekort != null) {
                return sak.meldeperioder.find { it.fraOgMed > sisteMeldekort.periode.tilOgMed }
            }

            return sak.meldeperioder.firstOrNull()
        }
    }

    data class UtenSak(
        val fnr: Fnr,
        val arenaMeldekortStatus: ArenaMeldekortStatus,
    ) : Bruker
}
