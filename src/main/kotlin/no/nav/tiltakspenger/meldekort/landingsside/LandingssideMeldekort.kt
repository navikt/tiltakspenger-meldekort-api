package no.nav.tiltakspenger.meldekort.landingsside

import java.time.LocalDateTime

data class LandingssideMeldekort(
    val kanSendesFra: LocalDateTime,
)

internal fun List<LandingssideMeldekort>.sortert(): List<LandingssideMeldekort> = sortedBy { it.kanSendesFra }
