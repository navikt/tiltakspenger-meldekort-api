package no.nav.tiltakspenger.meldekort.clients.saksbehandling

import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO
import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO.Status
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import java.time.LocalDate

fun Meldekort.toSaksbehandlingMeldekortDTO(): BrukerutfyltMeldekortDTO {
    return BrukerutfyltMeldekortDTO(
        id = this.id.toString(),
        sakId = this.sakId.toString(),
        periode = this.periode.toDTO(),
        meldeperiodeId = this.meldeperiode.id.toString(),
        mottatt = this.mottatt!!, // TODO: ikke bang bang!!
        dager = this.dager.toSaksbehandlingDTO(),
        journalpostId = this.journalpostId.toString(),
    )
}

fun List<MeldekortDag>.toSaksbehandlingDTO(): Map<LocalDate, Status> {
    return this.associate { dag ->
        dag.dag to when (dag.status) {
            MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET -> Status.DELTATT_UTEN_LØNN_I_TILTAKET
            MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET -> Status.DELTATT_MED_LØNN_I_TILTAKET
            MeldekortDagStatus.FRAVÆR_SYK -> Status.FRAVÆR_SYK
            MeldekortDagStatus.FRAVÆR_SYKT_BARN -> Status.FRAVÆR_SYKT_BARN
            MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV -> Status.FRAVÆR_GODKJENT_AV_NAV
            MeldekortDagStatus.FRAVÆR_ANNET -> Status.FRAVÆR_ANNET
            MeldekortDagStatus.IKKE_BESVART -> Status.IKKE_BESVART
            MeldekortDagStatus.IKKE_TILTAKSDAG -> Status.IKKE_TILTAKSDAG
            MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> Status.IKKE_RETT_TIL_TILTAKSPENGER
        }
    }
}
