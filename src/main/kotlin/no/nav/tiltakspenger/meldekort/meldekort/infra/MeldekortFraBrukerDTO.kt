package no.nav.tiltakspenger.meldekort.meldekort.infra

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.meldekort.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDag
import java.time.LocalDate

data class MeldekortFraBrukerDTO(
    val id: String,
    val dager: List<MeldekortDagFraBrukerDTO>,
    val locale: String?,
) {
    fun tilLagreKommando(fnr: Fnr): LagreMeldekortFraBrukerKommando {
        return LagreMeldekortFraBrukerKommando(
            id = MeldekortId.fromString(id),
            fnr = fnr,
            dager = dager.map { it.tilMeldekortDag() },
            locale = locale,
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
