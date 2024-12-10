package no.nav.tiltakspenger.meldekort.clients.saksbehandling

import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus

enum class MeldekortStatusDTO {
    KAN_UTFYLLES,
    KAN_IKKE_UTFYLLES,
    INNSENDT,
    GODKJENT,
}

fun MeldekortStatus.toDTO(): MeldekortStatusDTO {
    return when (this) {
        MeldekortStatus.KAN_UTFYLLES -> MeldekortStatusDTO.KAN_UTFYLLES
        MeldekortStatus.INNSENDT -> MeldekortStatusDTO.INNSENDT
        MeldekortStatus.KAN_IKKE_UTFYLLES -> MeldekortStatusDTO.KAN_IKKE_UTFYLLES
        MeldekortStatus.GODKJENT -> MeldekortStatusDTO.GODKJENT
    }
}
