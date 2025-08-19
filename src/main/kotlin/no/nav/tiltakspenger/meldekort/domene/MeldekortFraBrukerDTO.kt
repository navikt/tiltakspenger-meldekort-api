package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Command-delen av CQRS for meldekort. Brukes for å lagre meldekort som bruker har fylt ut.
 *
 * @param id Id til spesifikk versjon av meldeperioden på denne saken.
 */
data class LagreMeldekortFraBrukerKommando(
    val id: MeldekortId,
    val fnr: Fnr,
    val dager: List<MeldekortDagFraBrukerDTO>,
    val mottatt: LocalDateTime,
)

data class MeldekortFraBrukerDTO(
    val id: String,
    val dager: List<MeldekortDagFraBrukerDTO>,
) {
    fun tilLagreKommando(fnr: Fnr, clock: Clock): LagreMeldekortFraBrukerKommando {
        return LagreMeldekortFraBrukerKommando(
            id = MeldekortId.fromString(id),
            fnr = fnr,
            dager = dager,
            mottatt = nå(clock),
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
