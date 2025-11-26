package no.nav.tiltakspenger.meldekort.routes.meldekort.microfrontend

import java.time.LocalDateTime

data class MicrofrontendKortDTO(
    val antallMeldekortKlarTilInnsending: Int,
    val nesteMuligeInnsendingstidspunkt: LocalDateTime?,
)
