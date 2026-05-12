package no.nav.tiltakspenger.meldekort.meldekort

import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode

data class MeldekortMedSisteMeldeperiode(
    val meldekort: BrukersMeldekort,
    val sisteMeldeperiode: Meldeperiode,
)
