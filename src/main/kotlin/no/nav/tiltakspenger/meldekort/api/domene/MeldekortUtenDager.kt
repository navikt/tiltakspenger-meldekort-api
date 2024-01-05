package no.nav.tiltakspenger.meldekort.api.domene

import no.nav.tiltakspenger.meldekort.api.routes.dto.StatusDTO
import java.time.LocalDate
import java.util.UUID

data class MeldekortUtenDager(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: StatusDTO,
)
