package no.nav.tiltakspenger.meldekort.api.routes.dto

import java.time.LocalDate

data class MeldekortDagDTO(
    val meldekortId: String,
    val dato: LocalDate,
    val status: String,
)
