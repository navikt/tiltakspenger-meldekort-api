package no.nav.tiltakspenger.meldekort.microfrontend.infra.routes

import java.time.LocalDateTime

data class MicrofrontendKortDTO(
    val antallMeldekortKlarTilInnsending: Int,
    val nesteMuligeInnsendingstidspunkt: LocalDateTime?,
)
