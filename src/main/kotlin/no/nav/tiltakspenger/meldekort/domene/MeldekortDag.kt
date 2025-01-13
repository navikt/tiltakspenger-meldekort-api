package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.meldekort.routes.meldekort.MeldekortDagDTO
import java.time.LocalDate

data class MeldekortDag(
    val dag: LocalDate,
    val status: MeldekortDagStatus,
    val tiltakstype: TiltakstypeSomGirRett,
)

fun MeldekortDagDTO.toMeldekortDag(): MeldekortDag {
    return MeldekortDag(
        dag = this.dag,
        status = toMeldekortDagStatus(status),
        tiltakstype = this.tiltakstype,
    )
}

fun List<MeldekortDagDTO>.toMeldekortDager(): List<MeldekortDag> {
    return this.map { it.toMeldekortDag() }
}
