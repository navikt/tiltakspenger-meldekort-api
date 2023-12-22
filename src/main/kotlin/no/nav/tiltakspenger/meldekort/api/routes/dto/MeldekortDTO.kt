package no.nav.tiltakspenger.meldekort.api.routes.dto

import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag

data class MeldekortDTO(
    val grunnlagsData: MeldekortGrunnlag,
    val meldekort: List<Meldekort>,
)
