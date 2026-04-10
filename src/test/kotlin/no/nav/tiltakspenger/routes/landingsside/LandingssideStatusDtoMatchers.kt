package no.nav.tiltakspenger.routes.landingsside

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside.LandingssideStatusDTO
import no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside.LandingssideStatusDTO.LandingssideMeldekortDTO
import java.time.LocalDateTime

fun LandingssideStatusDTO.shouldBe(
    harInnsendteMeldekort: Boolean = false,
    meldekortTilUtfylling: List<LocalDateTime> = emptyList(),
    redirectUrl: String = Configuration.meldekortFrontendUrl,
) {
    this.shouldBe(
        LandingssideStatusDTO(
            harInnsendteMeldekort = harInnsendteMeldekort,
            meldekortTilUtfylling = meldekortTilUtfylling.map {
                LandingssideMeldekortDTO(it)
            },
        ),
    )
    this.redirectUrl shouldBe redirectUrl
}
