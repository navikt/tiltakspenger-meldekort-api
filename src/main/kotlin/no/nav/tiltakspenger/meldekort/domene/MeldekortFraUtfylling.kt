package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.meldekort.routes.meldekort.MeldekortFraUtfyllingDTO

class MeldekortFraUtfylling(
    val id: HendelseId,
    val meldekortDager: List<MeldekortDag>,
)

fun MeldekortFraUtfyllingDTO.tilMeldekortFraUtfylling(): MeldekortFraUtfylling {
    return MeldekortFraUtfylling(
        id = HendelseId.fromString(id),
        meldekortDager = meldekortDager.toMeldekortDager(),
    )
}
