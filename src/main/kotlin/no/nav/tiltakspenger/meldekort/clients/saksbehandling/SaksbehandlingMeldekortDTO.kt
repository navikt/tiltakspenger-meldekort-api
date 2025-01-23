package no.nav.tiltakspenger.meldekort.clients.saksbehandling

import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO
import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO.Status
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import java.time.LocalDate

fun BrukersMeldekort.toSaksbehandlingMeldekortDTO(): BrukerutfyltMeldekortDTO {
    return BrukerutfyltMeldekortDTO(
        id = this.id.toString(),
        sakId = this.sakId.toString(),
        periode = this.periode.toDTO(),
        meldeperiodeId = this.id.toString(),
        mottatt = this.mottatt,
        dager = this.dager.toSaksbehandlingDTO(),
    )
}

fun List<BrukersMeldekortDag>.toSaksbehandlingDTO(): Map<LocalDate, Status> {
    return this.associate { dag ->
        dag.dag to when (dag.status) {
            MeldekortDagStatus.DELTATT -> Status.DELTATT
            MeldekortDagStatus.FRAVÆR_SYK -> Status.FRAVÆR_SYK
            MeldekortDagStatus.FRAVÆR_SYKT_BARN -> Status.FRAVÆR_SYKT_BARN
            MeldekortDagStatus.FRAVÆR_ANNET -> Status.FRAVÆR_ANNET
            MeldekortDagStatus.IKKE_DELTATT -> Status.IKKE_DELTATT
            MeldekortDagStatus.IKKE_REGISTRERT -> Status.IKKE_REGISTRERT
            MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER -> Status.IKKE_RETT_TIL_TILTAKSPENGER
        }
    }
}
