package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import java.time.LocalDate

/**
 * Command-delen av CQRS for meldekort. Brukes for å lagre meldekort som bruker har fylt ut.
 *
 * @param id Id til spesifikk versjon av meldeperioden på denne saken.
 */
data class LagreMeldekortFraBrukerKommando(
    val id: MeldekortId,
    val fnr: Fnr,
    val dager: List<MeldekortDagFraBrukerDTO>,
)

data class MeldekortFraBrukerDTO(
    val id: String,
    val dager: List<MeldekortDagFraBrukerDTO>,
) {
    fun tilLagreKommando(fnr: Fnr): LagreMeldekortFraBrukerKommando {
        return LagreMeldekortFraBrukerKommando(
            id = MeldekortId.fromString(id),
            fnr = fnr,
            dager = dager,
        )
    }
}

data class MeldekortDagFraBrukerDTO(
    val dag: LocalDate,
    val status: MeldekortDagStatusDTO,
) {
    fun tilMeldekortDag(): MeldekortDag = MeldekortDag(
        dag = dag,
        status = status.tilMeldekortDagStatus(),
    )
}
