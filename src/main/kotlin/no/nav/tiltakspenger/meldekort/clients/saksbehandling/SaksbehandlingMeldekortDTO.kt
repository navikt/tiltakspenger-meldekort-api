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
            MeldekortDagStatus.DELTATT -> Status.DELTATT
            MeldekortDagStatus.FRAVÆR_SYK -> Status.FRAVÆR_SYK
            MeldekortDagStatus.FRAVÆR_SYKT_BARN -> Status.FRAVÆR_SYKT_BARN
            MeldekortDagStatus.FRAVÆR_ANNET -> Status.FRAVÆR_ANNET
            MeldekortDagStatus.IKKE_DELTATT -> Status.IKKE_DELTATT
            MeldekortDagStatus.IKKE_REGISTRERT -> Status.IKKE_REGISTRERT
        }
    }
}
