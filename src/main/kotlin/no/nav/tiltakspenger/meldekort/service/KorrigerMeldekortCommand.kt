package no.nav.tiltakspenger.meldekort.service

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag

data class KorrigerMeldekortCommand(
    val meldekortId: MeldekortId,
    val fnr: Fnr,
    val korrigerteDager: List<MeldekortDag>,
    val locale: String?,
)
