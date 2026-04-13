package no.nav.tiltakspenger.routes.microfrontend

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.meldekort.routes.meldekort.microfrontend.MicrofrontendKortDTO
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
