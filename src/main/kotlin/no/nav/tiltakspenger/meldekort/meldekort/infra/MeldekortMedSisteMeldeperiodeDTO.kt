package no.nav.tiltakspenger.meldekort.meldekort.infra

import no.nav.tiltakspenger.meldekort.meldekort.MeldekortMedSisteMeldeperiode
import no.nav.tiltakspenger.meldekort.meldeperiode.infra.MeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.meldeperiode.infra.tilMeldeperiodeDTO
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
