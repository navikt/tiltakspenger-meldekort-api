package no.nav.tiltakspenger.meldekort.api.routes.dto

import java.time.LocalDate

data class MeldekortUtenDagerDTO(
    val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: String,
)
