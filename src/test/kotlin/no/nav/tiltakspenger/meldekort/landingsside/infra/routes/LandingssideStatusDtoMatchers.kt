package no.nav.tiltakspenger.meldekort.landingsside.infra.routes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.meldekort.infra.Configuration
import no.nav.tiltakspenger.meldekort.landingsside.infra.routes.LandingssideStatusDTO.LandingssideMeldekortDTO
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
            redirectUrl = "http://localhost:2223/tiltakspenger/meldekort",
        ),
    )
    this.redirectUrl shouldBe redirectUrl
}
