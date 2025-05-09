package no.nav.tiltakspenger.meldekort.domene

data class AlleMeldekortDTO(
    val bruker: BrukerDTO,
    val meldekort: List<MeldekortTilBrukerDTO>,
)
