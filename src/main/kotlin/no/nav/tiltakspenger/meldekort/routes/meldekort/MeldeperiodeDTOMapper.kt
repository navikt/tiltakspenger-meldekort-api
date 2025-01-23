package no.nav.tiltakspenger.meldekort.routes.meldekort

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode

fun MeldeperiodeDTO.tilMeldeperiode(): Meldeperiode {
    return Meldeperiode(
        id = this.id,
        kjedeId = this.meldeperiodeKjedeId,
        versjon = this.versjon,
        sakId = SakId.fromString(this.sakId),
        saksnummer = this.saksnummer,
        fnr = Fnr.fromString(this.fnr),
        periode = Periode(this.fraOgMed, this.tilOgMed),
        opprettet = this.opprettet,
        maksAntallDagerForPeriode = this.antallDagerForPeriode,
        girRett = this.girRett,
    )
}
