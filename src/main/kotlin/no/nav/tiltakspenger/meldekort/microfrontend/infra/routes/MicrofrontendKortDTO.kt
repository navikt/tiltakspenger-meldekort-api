package no.nav.tiltakspenger.meldekort.microfrontend.infra.routes

import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendMeldekortInfo
import java.time.LocalDateTime

/**
 * Serialiserer [MicrofrontendMeldekortInfo] til JSON-en som sendes til brukers meldekort-microfrontend.
 *
 * Dette er den eneste offentlige inngangen til [MicrofrontendKortDTO].
 * Selve DTO-en er privat slik at den ikke lekker ut av denne fila, og slik at mappingen fra domenet holdes her.
 */
fun MicrofrontendMeldekortInfo.toDTO(): String = serialize(
    MicrofrontendKortDTO(
        antallMeldekortKlarTilInnsending = antallMeldekortKlarTilInnsending,
        nesteMuligeInnsendingstidspunkt = nesteMuligeInnsendingstidspunkt,
    ),
)

private data class MicrofrontendKortDTO(
    val antallMeldekortKlarTilInnsending: Int,
    val nesteMuligeInnsendingstidspunkt: LocalDateTime?,
)
