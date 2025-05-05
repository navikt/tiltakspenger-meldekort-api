package no.nav.tiltakspenger.meldekort.clients.pdfgen

import no.nav.tiltakspenger.meldekort.clients.utils.toNorskUkedagOgDatoUtenÅr
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus

class BrevMeldekortDagDTO(
    val dag: String,
    val status: BrevMeldekortStatusDTO,
)

enum class BrevMeldekortStatusDTO {
    DELTATT_UTEN_LØNN_I_TILTAKET,
    DELTATT_MED_LØNN_I_TILTAKET,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_VELFERD_GODKJENT_AV_NAV,
    FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV,
    IKKE_REGISTRERT,
}

private fun MeldekortDagStatus.tilBrevMeldekortStatusDTO(): BrevMeldekortStatusDTO = when (this) {
    MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> BrevMeldekortStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
    MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> BrevMeldekortStatusDTO.DELTATT_MED_LØNN_I_TILTAKET
    MeldekortDagStatus.FRAVÆR_SYK -> BrevMeldekortStatusDTO.FRAVÆR_SYK
    MeldekortDagStatus.FRAVÆR_SYKT_BARN -> BrevMeldekortStatusDTO.FRAVÆR_SYKT_BARN
    MeldekortDagStatus.FRAVÆR_VELFERD_GODKJENT_AV_NAV -> BrevMeldekortStatusDTO.FRAVÆR_VELFERD_GODKJENT_AV_NAV
    MeldekortDagStatus.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV -> BrevMeldekortStatusDTO.FRAVÆR_VELFERD_IKKE_GODKJENT_AV_NAV
    MeldekortDagStatus.IKKE_REGISTRERT -> BrevMeldekortStatusDTO.IKKE_REGISTRERT
}

fun List<MeldekortDag>.tilBrevMeldekortDagDTO(): List<BrevMeldekortDagDTO> {
    return this.map {
        BrevMeldekortDagDTO(
            dag = it.dag.toNorskUkedagOgDatoUtenÅr(),
            status = it.status.tilBrevMeldekortStatusDTO(),
        )
    }
}
