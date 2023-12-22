package no.nav.tiltakspenger.meldekort.api.routes.dto

import no.nav.tiltakspenger.meldekort.api.domene.MeldekortDag
import java.time.LocalDate

data class MeldekortMedTiltakDTO(
    val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: String,
    val tiltak: List<TiltakDTO>,
    val meldekortdager: List<MeldekortDag>,
)
