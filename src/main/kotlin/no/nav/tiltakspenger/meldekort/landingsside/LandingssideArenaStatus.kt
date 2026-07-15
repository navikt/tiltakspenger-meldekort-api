package no.nav.tiltakspenger.meldekort.landingsside

data class LandingssideArenaStatus(
    val harInnsendteMeldekort: Boolean,
    val meldekortTilUtfylling: List<LandingssideMeldekort>,
) {
    init {
        require(meldekortTilUtfylling == meldekortTilUtfylling.sortert()) {
            "Meldekort til utfylling må være sortert på kanSendesFra"
        }
    }

    fun tilLandingssideStatus(redirectUrl: String): LandingssideStatus = LandingssideStatus(
        harInnsendteMeldekort = harInnsendteMeldekort,
        meldekortTilUtfylling = meldekortTilUtfylling,
        redirectUrl = redirectUrl,
    )

    companion object {
        /**
         * Brukeren har en arena-status å vise dersom de enten har innsendte meldekort i arena, eller har tiltakspenger-meldekort i arena (uavhengig av om de er klare til utfylling).
         * Returnerer null når det ikke finnes noe relevant i arena.
         */
        fun create(
            harMeldekortIArena: Boolean,
            harInnsendteMeldekort: Boolean,
            meldekortTilUtfylling: List<LandingssideMeldekort>,
        ): LandingssideArenaStatus? {
            if (!harInnsendteMeldekort && !harMeldekortIArena) {
                return null
            }

            return LandingssideArenaStatus(
                harInnsendteMeldekort = harInnsendteMeldekort,
                meldekortTilUtfylling = meldekortTilUtfylling.sortert(),
            )
        }
    }
}
