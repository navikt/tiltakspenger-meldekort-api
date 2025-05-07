package no.nav.tiltakspenger.meldekort.domene

data class AlleMeldekortDTO(
    val meldekort: List<MeldekortTilBrukerDTO>,
    val bruker: BrukerDTO,
)
