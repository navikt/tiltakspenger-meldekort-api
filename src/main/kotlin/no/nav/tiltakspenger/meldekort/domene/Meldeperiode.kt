package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
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
    val id: String,
    val kjedeId: String,
    val versjon: Int,
    val sakId: SakId,
    val saksnummer: String,
    val fnr: Fnr,
    val periode: Periode,
    val opprettet: LocalDateTime,
    val maksAntallDagerForPeriode: Int,
    val girRett: Map<LocalDate, Boolean>,
) {

    val status: MeldekortStatus = if (girRett.any { it.value }) MeldekortStatus.KAN_UTFYLLES else MeldekortStatus.KAN_IKKE_UTFYLLES

    init {
        require(versjon >= 0) { "Versjon må være større eller lik 0" }
        require(girRett.size.toLong() == periode.antallDager) { "GirRett må ha like mange dager som perioden" }
        require(maksAntallDagerForPeriode >= 1) { "MaksAntallDagerForPeriode må være større eller lik 1" }
        require(maksAntallDagerForPeriode <= periode.antallDager) { "MaksAntallDagerForPeriode må være mindre eller lik dager i perioden" }
        require(periode.tilDager() == girRett.keys) { "GirRett må ha en verdi for hver dag i perioden" }
    }
}
