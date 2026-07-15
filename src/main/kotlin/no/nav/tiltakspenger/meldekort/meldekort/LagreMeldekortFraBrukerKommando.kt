package no.nav.tiltakspenger.meldekort.meldekort

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId

/**
 * Command-delen av CQRS for meldekort.
 * Brukes for å lagre meldekort som bruker har fylt ut.
 *
 * @param id Id til spesifikk versjon av meldeperioden på denne saken.
 */
data class LagreMeldekortFraBrukerKommando(
    val id: MeldekortId,
    val fnr: Fnr,
    val dager: List<MeldekortDag>,
    val locale: String?,
)
