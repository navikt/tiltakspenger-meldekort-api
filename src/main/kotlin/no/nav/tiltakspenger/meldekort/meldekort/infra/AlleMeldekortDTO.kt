package no.nav.tiltakspenger.meldekort.meldekort.infra

import no.nav.tiltakspenger.meldekort.bruker.infra.routes.BrukerDTO

data class AlleMeldekortDTO(
    val bruker: BrukerDTO,
    val meldekortMedSisteMeldeperiode: List<MeldekortMedSisteMeldeperiodeDTO>,
)
