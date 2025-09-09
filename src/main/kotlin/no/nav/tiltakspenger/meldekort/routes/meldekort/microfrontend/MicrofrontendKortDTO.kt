package no.nav.tiltakspenger.meldekort.routes.meldekort.microfrontend

import java.time.LocalDate

data class MicrofrontendKortDTO(
    val antallMeldekortKlarTilInnsending: Int,
    val nesteMuligeInnsending: LocalDate?,
)
