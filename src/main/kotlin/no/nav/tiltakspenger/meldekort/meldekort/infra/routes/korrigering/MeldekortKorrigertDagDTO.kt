package no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering

import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import java.time.LocalDate

data class MeldekortKorrigertDagDTO(
    val dato: LocalDate,
    val status: MeldekortDagStatus,
)
