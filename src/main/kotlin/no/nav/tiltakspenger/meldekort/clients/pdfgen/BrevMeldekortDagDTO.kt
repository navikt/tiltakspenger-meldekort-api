package no.nav.tiltakspenger.meldekort.clients.pdfgen

import no.nav.tiltakspenger.meldekort.clients.utils.toNorskUkedagOgDatoUtenÅr
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus

class BrevMeldekortDagDTO(
    val dag: String,
    val status: MeldekortDagStatus,
)

internal fun List<MeldekortDag>.toDTO(): List<BrevMeldekortDagDTO> {
    return this.map {
        BrevMeldekortDagDTO(
            dag = it.dag.toNorskUkedagOgDatoUtenÅr(),
            status = it.status,
        )
    }
}
