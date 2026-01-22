package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker

import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import java.time.LocalDate

data class MeldekortKorrigertDagDTO(
    val dato: LocalDate,
    val status: MeldekortDagStatus,
)
