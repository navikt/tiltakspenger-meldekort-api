package no.nav.tiltakspenger.meldekort.clients.utils

import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.libs.periodisering.norskDatoMedPunktumFormatter
import no.nav.tiltakspenger.libs.periodisering.norskUkedagOgDatoUtenÅrFormatter
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

private val locale: Locale? = Locale.of("nb", "NO")
private val uke = WeekFields.of(locale).weekOfWeekBasedYear()

fun LocalDate.toNorskUkenummer() = this.get(uke)
fun LocalDate.toNorskDato() = this.format(norskDatoFormatter)
fun LocalDate.toNorskDatoMedPunktum() = this.format(norskDatoMedPunktumFormatter)
fun LocalDate.toNorskUkedagOgDatoUtenÅr() = this.format(norskUkedagOgDatoUtenÅrFormatter).replaceFirstChar { it.uppercase() }
