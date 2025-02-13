package no.nav.tiltakspenger.meldekort.clients.utils

import no.nav.tiltakspenger.libs.periodisering.norskDatoFormatter
import no.nav.tiltakspenger.libs.periodisering.norskDatoMedPunktumFormatter
import no.nav.tiltakspenger.libs.periodisering.norskDatoOgTidFormatter
import no.nav.tiltakspenger.libs.periodisering.norskUkedagOgDatoUtenÅrFormatter
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
