package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.routes.meldekort.MeldekortFraUtfyllingDTO

class MeldekortFraUtfylling(
    val id: MeldekortId,
    val meldekortDager: List<MeldekortDag>,
)

fun MeldekortFraUtfyllingDTO.tilMeldekortFraUtfylling(): MeldekortFraUtfylling {
    return MeldekortFraUtfylling(
        id = MeldekortId.fromString(id),
        meldekortDager = meldekortDager.toMeldekortDager(),
    )
}
