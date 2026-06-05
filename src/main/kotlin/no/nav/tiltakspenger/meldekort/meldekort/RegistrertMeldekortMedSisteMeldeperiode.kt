package no.nav.tiltakspenger.meldekort.meldekort

import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode

/**
 * Kjedens siste registrerte tilstand ([RegistrertMeldekort]) sammen med kjedens siste [Meldeperiode].
 *
 * Brukes i «allerede utfylt»-listen mot bruker, der både digitale innsendinger og
 * meldekortvedtak (papirmeldekort) skal vises.
 */
data class RegistrertMeldekortMedSisteMeldeperiode(
    val registrertMeldekort: RegistrertMeldekort,
    val sisteMeldeperiode: Meldeperiode,
)
