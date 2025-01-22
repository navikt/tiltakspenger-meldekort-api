package no.nav.tiltakspenger.meldekort.routes.meldekort

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDag
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus

fun MeldeperiodeDTO.tilMeldekort(): Meldekort {
    return Meldekort(
        // TODO Anders og John: Blir denne til MeldeperiodeId?
        id = HendelseId.fromString(this.id),
        fnr = Fnr.fromString(this.fnr),
        periode = Periode(this.fraOgMed, this.tilOgMed),
        meldeperiodeKjedeId = MeldeperiodeId(this.meldeperiodeKjedeId),
        dager = this.girRett.map {
            MeldekortDag(
                dag = it.key,
                status = if (it.value) MeldekortDagStatus.IKKE_REGISTRERT else MeldekortDagStatus.SPERRET,
            )
        }.toNonEmptyListOrNull()!!,
        status = if (this.girRett.any { it.value }) MeldekortStatus.KAN_UTFYLLES else MeldekortStatus.KAN_IKKE_UTFYLLES,
    )
}
