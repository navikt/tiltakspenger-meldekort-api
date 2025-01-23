package no.nav.tiltakspenger.meldekort.domene

import arrow.core.NonEmptyList

/**
 * Command-delen av CQRS for meldekort. Brukes for å lagre meldekort som bruker har fylt ut.
 *
 * @param meldeperiodeId Id til spesifikk versjon av meldeperioden på denne saken.
 */
data class MeldekortFraUtfylling(
    val meldeperiodeId: String,
    val meldeperiodeKjedeId: String,
    val meldekortDager: NonEmptyList<BrukersMeldekortDag>,
)
