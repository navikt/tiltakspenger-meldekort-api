package no.nav.tiltakspenger.meldekort.landingsside

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus

data class LandingssideSak(
    val fnr: Fnr,
    val arenaMeldekortStatus: ArenaMeldekortStatus,
    val harInnsendteMeldekort: Boolean,
    val meldekortTilUtfylling: List<LandingssideMeldekort>,
) {
    init {
        require(meldekortTilUtfylling == meldekortTilUtfylling.sortert()) {
            "Meldekort til utfylling må være sortert på kanSendesFra"
        }
    }

    /**
     * Vil være true dersom [ArenaMeldekortStatus.HAR_IKKE_MELDEKORT] og false dersom [ArenaMeldekortStatus.HAR_MELDEKORT] eller [ArenaMeldekortStatus.UKJENT].
     */
    val harIkkeMeldekortIArena: Boolean = arenaMeldekortStatus == ArenaMeldekortStatus.HAR_IKKE_MELDEKORT

    fun tilLandingssideStatus(
        arenaStatus: LandingssideArenaStatus?,
        redirectUrl: String,
    ): LandingssideStatus = LandingssideStatus(
        harInnsendteMeldekort = harInnsendteMeldekort || arenaStatus?.harInnsendteMeldekort == true,
        meldekortTilUtfylling = meldekortTilUtfylling.plus(arenaStatus?.meldekortTilUtfylling.orEmpty()).sortert(),
        redirectUrl = redirectUrl,
    )
}
