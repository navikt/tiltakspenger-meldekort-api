package no.nav.tiltakspenger.meldekort.domene

data class AlleMeldekortDTO(
    val bruker: BrukerDTO,
    @Deprecated("meldekort erstattes av meldekortMedSisteMeldeperiode, men begge finnes en kort periode frem til frontend er oppdatert")
    val meldekort: List<MeldekortTilBrukerDTO>,
    val meldekortMedSisteMeldeperiode: List<MeldekortMedSisteMeldeperiodeDTO>,
)
