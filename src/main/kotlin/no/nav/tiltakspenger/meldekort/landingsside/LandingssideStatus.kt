package no.nav.tiltakspenger.meldekort.landingsside

import java.time.LocalDateTime

data class LandingssideStatus(
    val harInnsendteMeldekort: Boolean,
    val meldekortTilUtfylling: List<LandingssideMeldekort>,
    val redirectUrl: String,
)

data class LandingssideMeldekort(
    val kanSendesFra: LocalDateTime,
)
