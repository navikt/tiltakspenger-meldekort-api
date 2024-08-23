package no.nav.tiltakspenger.meldekort.api.routes.dto

import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import java.time.LocalDate

data class MeldekortDTO(
    val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val meldekortDager: List<MeldekortDagDTO>,
    // TODO Kew: Må få på antall dager per uke når vi trenger det.
//    val antallDagerPerUke: Int,
)

fun Meldekort.toDTO(): MeldekortDTO =
    MeldekortDTO(
        id = id.toString(),
        fom = fom,
        tom = tom,
        meldekortDager = meldekortDager.toDTO(),
    )
