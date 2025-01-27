package no.nav.tiltakspenger.meldekort.routes.meldekort

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.MeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.toDTO
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortFraUtfylling
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldekortTilUtfyllingDTO(
    val id: String,
    val meldeperiodeId: String,
    val meldeperiodeKjedeId: String,
    val versjon: Int,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val status: MeldekortStatusDTO,
    val innsendt: LocalDateTime?,
    val dager: List<MeldekortDagDTO>,
)

data class MeldekortFraUtfyllingDTO(
    val id: String,
    val meldekortDager: List<MeldekortDagDTO>,
) {
    fun toDomain(): MeldekortFraUtfylling {
        return MeldekortFraUtfylling(
            id = MeldekortId.fromString(id),
            // TODO: valider dagene
            meldekortDager = meldekortDager.toMeldekortDager().toNonEmptyListOrNull()!!,
            mottatt = nå(),
        )
    }
}

fun BrukersMeldekort.tilUtfyllingDTO(): MeldekortTilUtfyllingDTO {
    return MeldekortTilUtfyllingDTO(
        id = this.id.toString(),
        meldeperiodeId = this.meldeperiode.id,
        meldeperiodeKjedeId = this.meldeperiode.kjedeId,
        versjon = this.meldeperiode.versjon,
        fraOgMed = this.periode.fraOgMed,
        tilOgMed = this.periode.tilOgMed,
        status = this.status.toDTO(),
        innsendt = this.mottatt,
        dager = this.dager.toMeldekortDagDTO(),
    )
}
