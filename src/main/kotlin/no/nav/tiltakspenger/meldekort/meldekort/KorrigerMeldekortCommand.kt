package no.nav.tiltakspenger.meldekort.meldekort

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDag

data class KorrigerMeldekortCommand(
    val meldekortId: MeldekortId,
    val fnr: Fnr,
    val korrigerteDager: List<MeldekortDag>,
    val locale: String?,
)
