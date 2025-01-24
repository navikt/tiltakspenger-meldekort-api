package no.nav.tiltakspenger.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.MeldekortId
import java.time.LocalDateTime

/**
 * Command-delen av CQRS for meldekort. Brukes for å lagre meldekort som bruker har fylt ut.
 *
 * @param id Id til spesifikk versjon av meldeperioden på denne saken.
 */
data class MeldekortFraUtfylling(
    val id: MeldekortId,
    val meldekortDager: NonEmptyList<BrukersMeldekortDag>,
    val mottatt: LocalDateTime,
)
