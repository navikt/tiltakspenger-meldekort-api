package no.nav.tiltakspenger.meldekort.routes.meldekort

import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.toMeldekortDagStatus
import java.time.LocalDate

data class MeldekortDagDTO(
    val dag: LocalDate,
    val status: String,
)

fun List<BrukersMeldekortDag>.toMeldekortDagDTO(): List<MeldekortDagDTO> =
    this.map { dag ->
        MeldekortDagDTO(
            dag = dag.dag,
            status = when (dag.status) {
                MeldekortDagStatus.DELTATT -> "DELTATT"
                MeldekortDagStatus.FRAVÆR_SYK -> "FRAVÆR_SYK"
                MeldekortDagStatus.FRAVÆR_SYKT_BARN -> "FRAVÆR_SYKT_BARN"
                MeldekortDagStatus.FRAVÆR_ANNET -> "FRAVÆR_ANNET"
                MeldekortDagStatus.IKKE_REGISTRERT -> "IKKE_REGISTRERT"
                MeldekortDagStatus.IKKE_DELTATT -> "IKKE_DELTATT"
                MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> "SPERRET"
            },
        )
    }

fun Meldeperiode.toMeldekortDagDTOliste(): List<MeldekortDagDTO> =
    this.girRett.map { dag ->
        MeldekortDagDTO(dag = dag.key, status = if (dag.value) "IKKE_REGISTRERT" else "SPERRET")
    }

fun List<MeldekortDagDTO>.toMeldekortDager(): List<BrukersMeldekortDag> {
    return this.map { it.toMeldekortDag() }
}

fun MeldekortDagDTO.toMeldekortDag(): BrukersMeldekortDag {
    return BrukersMeldekortDag(
        dag = this.dag,
        status = toMeldekortDagStatus(this.status),
    )
}
