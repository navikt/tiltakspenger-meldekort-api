package no.nav.tiltakspenger.meldekort.meldeperiode

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.temporal.TemporalAdjusters

/**
 * Se også Meldeperiode i tiltakspenger-saksbehandling-api
 *
 * @param id ULID/UUID som unikt identifiserer denne versjonen av meldeperioden for denne saken.
 * @param kjedeId ULID/UUID som unikt identifiserer kjeden av meldeperioder for denne saken.
 * @param versjon Angir hvilken versjon meldeperioden dette er.
 *        Når vi får nye vedtak som påvirker en spesifikk meldeperiode, vil denne øke.
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

        /**
         * Validerer at meldeperiodene er sortert etter periode og versjon.
         * Brukes som invariant av både lesemodellen [no.nav.tiltakspenger.meldekort.sak.Sak] og skrivemodellen [no.nav.tiltakspenger.meldekort.mottak.MottattSak].
         */
        fun List<Meldeperiode>.krevSortertEtterPeriodeOgVersjon() {
            zipWithNext().forEach { (a, b) ->
                require(a.periode.tilOgMed < b.periode.fraOgMed || (a.periode == b.periode && a.versjon < b.versjon)) {
                    "Meldeperioder må være sortert etter periode og versjon. Fikk $a før $b"
                }
            }
        }

        /**
         * Meldekortet kan normalt fylles ut fra siste fredag i meldeperioden kl. 15.
         * Siste meldekort før jul kan fylles ut fra siste onsdag i meldeperioden kl. 15, slik at bruker får pengene før jul.
         * Er siste onsdag mindre enn 7 dager før julaften, kan det i stedet fylles ut fra siste hverdag som er minst 7 dager før julaften.
         * Se også migreringen V44, som oppdaterte meldeperioder som var mottatt før denne regelen ble innført.
         */
        fun Periode.kanFyllesUtFraOgMed(): LocalDateTime {
            val dato = if (this.erSisteMeldeperiodeFørJul()) {
                minOf(
                    this.tilOgMed.with(TemporalAdjusters.previousOrSame(DayOfWeek.WEDNESDAY)),
                    sisteHverdagMinst7DagerFørJulaften(this.tilOgMed.year),
                )
            } else {
                this.tilOgMed.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY))
            }
            return dato.atTime(TIDSPUNKT_BRUKER_KAN_FYLLE_UT_MELDEPERIODE_FOR).also {
                if (!this.inneholder(it.toLocalDate())) {
                    throw IllegalArgumentException("$it er utenfor perioden $this")
                }
            }
        }

        /**
         * Siste meldeperiode før jul er meldeperioden som slutter i løpet av de 14 siste dagene til og med julaften (11.–24. desember).
         * Siden meldeperiodene er 14 dager lange og følger etter hverandre, er det nøyaktig én slik meldeperiode hvert år.
         */
        private fun Periode.erSisteMeldeperiodeFørJul(): Boolean {
            val julaften = LocalDate.of(this.tilOgMed.year, Month.DECEMBER, 24)
            return this.tilOgMed > julaften.minusDays(14) && this.tilOgMed <= julaften
        }

        private fun sisteHverdagMinst7DagerFørJulaften(år: Int): LocalDate {
            val dato = LocalDate.of(år, Month.DECEMBER, 17)
            return when (dato.dayOfWeek) {
                DayOfWeek.SATURDAY -> dato.minusDays(1)
                DayOfWeek.SUNDAY -> dato.minusDays(2)
                else -> dato
            }
        }
    }
}
