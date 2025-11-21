package no.nav.tiltakspenger.meldekort.routes.meldekort.microfrontend

import java.time.LocalDate
import java.time.LocalDateTime

data class MicrofrontendKortDTO(
    val antallMeldekortKlarTilInnsending: Int,
    // TODO - slett dette feltet etter at vi har verifisert at dato med tidspunkt fungerer GOOD!
    val nesteMuligeInnsending: LocalDate?,
    val nesteMuligeInnsendingstidspunkt: LocalDateTime?,
)
