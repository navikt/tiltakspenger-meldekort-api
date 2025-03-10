package no.nav.tiltakspenger.meldekort.domene

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Se også Meldeperiode i tiltakspenger-saksbehandling-api
 *
 * @param id ULID/UUID som unikt identifiserer denne versjonen av meldeperioden for denne saken.
 * @param kjedeId ULID/UUID som unikt identifiserer kjeden av meldeperioder for denne saken.
 * @param versjon Angir hvilken versjon meldeperioden dette er. Når vi får nye vedtak som påvirker en spesifikk meldeperiode, vil denne øke.
 */
data class Meldeperiode(
    val id: MeldeperiodeId,
    val kjedeId: MeldeperiodeKjedeId,
    val versjon: Int,
    val sakId: SakId,
    val saksnummer: String?,
    val fnr: Fnr,
    val periode: Periode,
    val opprettet: LocalDateTime,
    val maksAntallDagerForPeriode: Int,
    val girRett: Map<LocalDate, Boolean>,
) {
    val harRettIPerioden = girRett.any { it.value }

    init {
        require(versjon >= 0) { "Versjon må være større eller lik 0" }
        require(girRett.size.toLong() == periode.antallDager) { "GirRett må ha like mange dager som perioden" }
        require(maksAntallDagerForPeriode <= periode.antallDager) { "MaksAntallDagerForPeriode må være mindre eller lik dager i perioden" }
        require(periode.tilDager() == girRett.keys.toList()) { "GirRett må ha en verdi for hver dag i perioden" }
    }
}

fun MeldeperiodeDTO.tilMeldeperiode(): Either<UgyldigMeldeperiode, Meldeperiode> {
    return Either.catch {
        Meldeperiode(
            id = MeldeperiodeId.fromString(this.id),
            kjedeId = MeldeperiodeKjedeId(this.kjedeId),
            versjon = this.versjon,
            sakId = SakId.fromString(this.sakId),
            saksnummer = this.saksnummer,
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
