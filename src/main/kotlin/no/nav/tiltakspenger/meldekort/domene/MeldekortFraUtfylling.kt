package no.nav.tiltakspenger.meldekort.domene

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.meldekort.routes.meldekort.MeldekortFraUtfyllingDTO
import no.nav.tiltakspenger.meldekort.routes.meldekort.toMeldekortDager

class MeldekortFraUtfylling(
    val id: HendelseId,
    val meldekortDager: NonEmptyList<MeldekortDag>,
)

fun MeldekortFraUtfyllingDTO.tilMeldekortFraUtfylling(): MeldekortFraUtfylling {
    return MeldekortFraUtfylling(
        id = HendelseId.fromString(id),
        meldekortDager = meldekortDager.toMeldekortDager().toNonEmptyListOrNull()!!,
    )
}
