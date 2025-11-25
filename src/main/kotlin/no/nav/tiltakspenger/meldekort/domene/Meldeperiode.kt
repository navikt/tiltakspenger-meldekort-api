package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.max

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
    val saksnummer: String,
    val fnr: Fnr,
    val periode: Periode,
    val opprettet: LocalDateTime,
    val maksAntallDagerForPeriode: Int,
    val girRett: Map<LocalDate, Boolean>,
    /**
     * Feltet indikerer 2 ting:
     * - Hvilken dato meldeperioden aksepterer at meldekort kan fylles ut fra.
     * - Hvilken dato vi skal sende varsel om at meldekort kan fylles ut fra.
     */
    val kanFyllesUtFraOgMed: LocalDateTime,
) {
    val alleDagerGirRettIPeriode = girRett.all { it.value }
    val minstEnDagGirRettIPerioden = girRett.any { it.value }
    val antallDagerSomIkkeGirRett: Int = girRett.count { !it.value }
    val antallDagerSomGirRett: Int = girRett.count { it.value }

    // TODO Ramzi og John: Fjern denne når vi har fjernet den fra frontend.
    val minAntallDagerForPeriode = max((maksAntallDagerForPeriode - antallDagerSomIkkeGirRett), 0)

    fun erLik(other: Meldeperiode): Boolean {
        // Enkelte felter er ikke relevante for å avgjøre om to saker er like, dermed kopierer vi disse feltene før sammenligningen
        return this.copy(
            kanFyllesUtFraOgMed = other.kanFyllesUtFraOgMed,
        ) == other
    }

    init {
        require(versjon >= 0) { "Versjon må være større eller lik 0" }
        require(girRett.size.toLong() == periode.antallDager) { "GirRett må ha like mange dager som perioden" }
        require(antallDagerSomGirRett <= periode.antallDager) { "Antall dager som gir rett må være mindre eller lik dager i perioden" }
        require(maksAntallDagerForPeriode <= antallDagerSomGirRett) { "maks antall dager for periode må være mindre eller lik antall dager som gir rett" }
        require(periode.tilDager() == girRett.keys.toList()) { "GirRett må ha en verdi for hver dag i perioden" }
    }

    companion object {
        private val TIDSPUNKT_BRUKER_KAN_FYLLE_UT_MELDEPERIODE_FOR = LocalTime.of(15, 0)

        fun Periode.kanFyllesUtFraOgMed(): LocalDateTime =
            this.tilOgMed.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
                .atTime(TIDSPUNKT_BRUKER_KAN_FYLLE_UT_MELDEPERIODE_FOR).also {
                    if (!this.inneholder(it.toLocalDate())) {
                        throw IllegalArgumentException("$it er utenfor perioden $this")
                    }
                }
    }
}
