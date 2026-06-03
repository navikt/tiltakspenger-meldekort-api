package no.nav.tiltakspenger.meldekort.landingsside

data class LandingssideStatus(
    val harInnsendteMeldekort: Boolean,
    val meldekortTilUtfylling: List<LandingssideMeldekort>,
    val redirectUrl: String,
) {
    init {
        require(meldekortTilUtfylling == meldekortTilUtfylling.sortert()) {
            "Meldekort til utfylling må være sortert på kanSendesFra"
        }
    }
}
