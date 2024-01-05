package no.nav.tiltakspenger.meldekort.api.felles

import com.google.common.collect.BoundType
import com.google.common.collect.DiscreteDomain
import com.google.common.collect.Range
import java.time.LocalDate

class LocalDateDiscreteDomain : DiscreteDomain<LocalDate>() {
    override fun next(value: LocalDate): LocalDate {
        return value.plusDays(1)
    }

    override fun previous(value: LocalDate): LocalDate {
        return value.minusDays(1)
    }

    override fun distance(start: LocalDate, end: LocalDate): Long {
        return start.until(end).days.toLong()
    }
}

class Periode(fra: LocalDate, til: LocalDate) {

    companion object {
        val domain = LocalDateDiscreteDomain()
    }

    val range: Range<LocalDate> = lagRangeFraFraOgTil(fra, til)

    private fun lagRangeFraFraOgTil(fra: LocalDate, til: LocalDate): Range<LocalDate> =
        when {
            fra == LocalDate.MIN && til == LocalDate.MAX -> Range.all<LocalDate>().canonical(domain)
            fra == LocalDate.MIN && til != LocalDate.MAX -> Range.atMost(til).canonical(domain)
            fra != LocalDate.MIN && til == LocalDate.MAX -> Range.atLeast(fra).canonical(domain)
            else -> Range.closed(fra, til).canonical(domain)
        }

    val fra: LocalDate
        get() = range.fraOgMed()
    val til: LocalDate
        get() = range.tilOgMed()

    fun overlapperMed(periode: Periode) = try {
        !this.range.intersection(periode.range).isEmpty
    } catch (iae: IllegalArgumentException) {
        false
    }

    fun overlappendePeriode(periode: Periode): Periode? = try {
        this.range.intersection(periode.range).toPeriode()
    } catch (e: Exception) {
        null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Periode) return false

        if (range != other.range) return false

        return true
    }

    override fun hashCode(): Int {
        return range.hashCode()
    }

    override fun toString(): String {
        return "Periode(range=$range)"
    }

    fun tilDager(): List<LocalDate> {
        return fra.datesUntil(til.plusDays(1)).toList()
    }
}

fun Range<LocalDate>.toPeriode(): Periode = Periode(this.fraOgMed(), this.tilOgMed())
fun Range<LocalDate>.fraOgMed(): LocalDate =
    if (this.hasLowerBound()) {
        if (this.lowerBoundType() == BoundType.CLOSED) this.lowerEndpoint() else this.lowerEndpoint().plusDays(1)
    } else {
        LocalDate.MIN
    }

fun Range<LocalDate>.tilOgMed(): LocalDate =
    if (this.hasUpperBound()) {
        if (this.upperBoundType() == BoundType.CLOSED) this.upperEndpoint() else this.upperEndpoint().minusDays(1)
    } else {
        LocalDate.MAX
    }
