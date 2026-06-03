package no.nav.tiltakspenger.meldekort.landingsside.infra.routes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.meldekort.infra.Configuration
import no.nav.tiltakspenger.meldekort.landingsside.infra.routes.LandingssideStatusResponsDTO.LandingssideMeldekortDTO
import java.time.LocalDateTime

internal fun LandingssideStatusResponsDTO.shouldBe(
    harInnsendteMeldekort: Boolean = false,
    meldekortTilUtfylling: List<LocalDateTime> = emptyList(),
    redirectUrl: String = Configuration.meldekortFrontendUrl,
) {
    this.shouldBe(
        LandingssideStatusResponsDTO(
            harInnsendteMeldekort = harInnsendteMeldekort,
            meldekortTilUtfylling = meldekortTilUtfylling.map {
                LandingssideMeldekortDTO(
                    kanSendesFra = it,
                    kanFyllesUtFra = it,
                    fristForInnsending = null,
                )
            },
            redirectUrl = redirectUrl,
        ),
    )
}
