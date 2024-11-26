package no.nav.tiltakspenger.meldekort.routes.meldekort

import java.time.LocalDate
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDager

data class MeldekortDagerDTO(
    val meldekortDager: List<MeldekortDagDTO>
)

data class MeldekortDagDTO(
    val dag: LocalDate,
    val status: String
)

fun MeldekortDag.toDTO(): MeldekortDagDTO = MeldekortDagDTO(dag = this.dag, status = this.status)

fun MeldekortDager.toDTO(): MeldekortDagerDTO =
    MeldekortDagerDTO(meldekortDager = this.meldekortDager.map { it.toDTO() })
