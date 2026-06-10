package no.nav.tiltakspenger.meldekort.microfrontend

import java.time.LocalDateTime

/**
 * Informasjon microfrontend-pakken trenger om en brukers meldekort. Eies av microfrontend-pakken
 * og hentes via [MicrofrontendRepo.hentMeldekortInfo] – ikke via meldekort-pakkens tjenester.
 *
 * @param antallMeldekortKlarTilInnsending Antall meldekort som kan sendes inn nå.
 * @param nesteMuligeInnsendingstidspunkt Tidspunktet neste meldekort til utfylling kan sendes inn,
 * eller `null` dersom bruker ikke har noe meldekort til utfylling.
 */
data class MicrofrontendMeldekortInfo(
    val antallMeldekortKlarTilInnsending: Int,
    val nesteMuligeInnsendingstidspunkt: LocalDateTime?,
)
