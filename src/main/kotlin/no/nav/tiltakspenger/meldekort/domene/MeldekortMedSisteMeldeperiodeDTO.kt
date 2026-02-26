package no.nav.tiltakspenger.meldekort.domene

import java.time.Clock

data class MeldekortMedSisteMeldeperiodeDTO(
    val meldekort: MeldekortTilBrukerDTO,
    val sisteMeldeperiode: MeldeperiodeDTO,
)

fun MeldekortMedSisteMeldeperiode.tilMeldekortMedSisteMeldeperiodeDTO(clock: Clock): MeldekortMedSisteMeldeperiodeDTO {
    return MeldekortMedSisteMeldeperiodeDTO(
        meldekort = meldekort.tilMeldekortTilBrukerDTO(clock),
        sisteMeldeperiode = sisteMeldeperiode.tilMeldeperiodeDTO(),
    )
}
