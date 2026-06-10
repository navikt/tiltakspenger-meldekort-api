package no.nav.tiltakspenger.meldekort.microfrontend.infra.routes

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendMeldekortInfo
import java.time.LocalDateTime

fun MicrofrontendMeldekortInfo.shouldBe(
    antallMeldekortKlarTilInnsending: Int = 0,
    nesteMuligeInnsendingstidspunkt: LocalDateTime? = null,
) {
    this shouldBe MicrofrontendMeldekortInfo(
        antallMeldekortKlarTilInnsending = antallMeldekortKlarTilInnsending,
        nesteMuligeInnsendingstidspunkt = nesteMuligeInnsendingstidspunkt,
    )
}
