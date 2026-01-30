package no.nav.tiltakspenger.meldekort.clients.utils

import no.nav.tiltakspenger.libs.dato.engelskDatoFormatter
import no.nav.tiltakspenger.libs.dato.engelskDatoMedPunktumFormatter
import no.nav.tiltakspenger.libs.dato.engelskDatoOgTidFormatter
import no.nav.tiltakspenger.libs.dato.engelskUkedagOgDatoUtenÅrFormatter
import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.dato.norskDatoMedPunktumFormatter
import no.nav.tiltakspenger.libs.dato.norskDatoOgTidFormatter
import no.nav.tiltakspenger.libs.dato.norskUkedagOgDatoUtenÅrFormatter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import java.util.Locale

private val locale: Locale? = Locale.of("nb", "NO")
private val uke = WeekFields.of(locale).weekOfWeekBasedYear()

fun LocalDate.toNorskUkenummer(): Int = this.get(uke)
fun LocalDate.toNorskDato(): String = this.format(norskDatoFormatter)
fun LocalDate.toNorskDatoMedPunktum(): String = this.format(norskDatoMedPunktumFormatter)
fun LocalDate.toNorskUkedagOgDatoUtenÅr(): String = this.format(norskUkedagOgDatoUtenÅrFormatter).replaceFirstChar { it.uppercase() }
fun LocalDateTime.toNorskDatoOgTid(): String = this.format(norskDatoOgTidFormatter)

private val localeEn: Locale? = Locale.of("en", "UK")
private val week = WeekFields.of(localeEn).weekOfWeekBasedYear()

fun LocalDate.toEngelskUkenummer(): Int = this.get(week)
fun LocalDate.toEngelskDato(): String = this.format(engelskDatoFormatter)
fun LocalDate.toEngelskDatoMedPunktum(): String = this.format(engelskDatoMedPunktumFormatter)
fun LocalDate.toEngelskUkedagOgDatoUtenÅr(): String = this.format(engelskUkedagOgDatoUtenÅrFormatter).replaceFirstChar { it.uppercase() }
fun LocalDateTime.toEngelskDatoOgTid(): String = this.format(engelskDatoOgTidFormatter)
