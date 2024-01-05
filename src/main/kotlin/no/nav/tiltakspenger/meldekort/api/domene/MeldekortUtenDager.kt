package no.nav.tiltakspenger.meldekort.api.domene

import java.time.LocalDate
import java.util.UUID

data class MeldekortUtenDager(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: MeldekortStatus,
)

enum class MeldekortStatus(status: String) {
    ÅPENT("Åpent"),
    INNSENDT("Innsendt"),
}
