package no.nav.tiltakspenger.meldekort.clients.saksbehandling

import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus

enum class MeldekortStatusDTO {
    Til_Utfylling,
    Innsendt
}

fun MeldekortStatus.toDTO() : MeldekortStatusDTO {
    return when (this){
        MeldekortStatus.Til_Utfylling -> MeldekortStatusDTO.Til_Utfylling
        MeldekortStatus.Innsendt -> MeldekortStatusDTO.Innsendt
    }
}
