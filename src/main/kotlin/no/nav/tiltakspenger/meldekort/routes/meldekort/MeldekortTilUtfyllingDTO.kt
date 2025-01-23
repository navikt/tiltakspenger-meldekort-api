package no.nav.tiltakspenger.meldekort.routes.meldekort

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.MeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.toDTO
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortFraUtfylling
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import java.time.LocalDate

data class MeldekortTilUtfyllingDTO(
    val meldekortId: String?,
    val meldeperiodeId: String,
    val meldeperiodeKjedeId: String,
    val versjon: Int,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val status: MeldekortStatusDTO,
    val meldekortDager: List<MeldekortDagDTO>,
)

data class MeldekortFraUtfyllingDTO(
    val meldeperiodeId: String,
    val meldeperiodeKjedeId: String,
    val meldekortDager: List<MeldekortDagDTO>,
) {
    fun toDomain(): MeldekortFraUtfylling {
        return MeldekortFraUtfylling(
            meldeperiodeId = meldeperiodeId,
            meldeperiodeKjedeId = meldeperiodeKjedeId,
            meldekortDager = meldekortDager.toMeldekortDager().toNonEmptyListOrNull()!!,
        )
    }
}

fun BrukersMeldekort.tilUtfyllingDTO(): MeldekortTilUtfyllingDTO {
    return MeldekortTilUtfyllingDTO(
        meldekortId = this.id.toString(),
        meldeperiodeId = this.meldeperiode.id,
        meldeperiodeKjedeId = this.meldeperiode.kjedeId,
        versjon = this.meldeperiode.versjon,
        fraOgMed = this.periode.fraOgMed,
        tilOgMed = this.periode.tilOgMed,
        status = this.status.toDTO(),
        meldekortDager = this.dager.toMeldekortDagDTO(),
    )
}

fun Meldeperiode.tilUtfyllingDTO(): MeldekortTilUtfyllingDTO {
    return MeldekortTilUtfyllingDTO(
        meldekortId = null,
        meldeperiodeId = this.id,
        meldeperiodeKjedeId = this.kjedeId,
        versjon = this.versjon,
        fraOgMed = this.periode.fraOgMed,
        tilOgMed = this.periode.tilOgMed,
        status = this.status.toDTO(),
        meldekortDager = this.toMeldekortDagDTOliste(),
    )
}
