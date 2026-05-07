package no.nav.tiltakspenger.meldekort.microfrontend.infra.routes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.meldekort.microfrontend.infra.routes.MicrofrontendKortDTO
import java.time.LocalDateTime

fun MicrofrontendKortDTO.shouldBe(
    antallMeldekortKlarTilInnsending: Int = 0,
    nesteMuligeInnsendingstidspunkt: LocalDateTime? = null,
) {
    this shouldBe MicrofrontendKortDTO(
        antallMeldekortKlarTilInnsending = antallMeldekortKlarTilInnsending,
        nesteMuligeInnsendingstidspunkt = nesteMuligeInnsendingstidspunkt,
    )
}
