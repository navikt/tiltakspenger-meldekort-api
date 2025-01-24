package no.nav.tiltakspenger.meldekort.routes.meldekort

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode

fun MeldeperiodeDTO.tilMeldeperiode(): Either<UgyldigMeldeperiode, Meldeperiode> {
    return Either.catch {
        Meldeperiode(
            id = this.id,
            kjedeId = this.meldeperiodeKjedeId,
            versjon = this.versjon,
            sakId = SakId.fromString(this.sakId),
            fnr = Fnr.fromString(this.fnr),
            periode = Periode(this.fraOgMed, this.tilOgMed),
            opprettet = this.opprettet,
            maksAntallDagerForPeriode = this.antallDagerForPeriode,
            girRett = this.girRett,
        ).right()
    }.getOrElse {
        return UgyldigMeldeperiode(it.message).left()
    }
}

data class UgyldigMeldeperiode(val message: String? = "Ukjent feil")
