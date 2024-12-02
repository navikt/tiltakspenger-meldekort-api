package no.nav.tiltakspenger.meldekort.clients.saksbehandling

import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus

enum class MeldekortStatusDTO {
    TilUtfylling,
    Innsendt,
}

fun MeldekortStatus.toDTO(): MeldekortStatusDTO {
    return when (this) {
        MeldekortStatus.TilUtfylling -> MeldekortStatusDTO.TilUtfylling
        MeldekortStatus.Innsendt -> MeldekortStatusDTO.Innsendt
    }
}
