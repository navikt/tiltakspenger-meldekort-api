package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object ObjectMother :
    MeldekortMother,
    MeldeperiodeMother {
    fun periode(fraSisteMandagFør: LocalDate = nå(fixedClock).toLocalDate()): Periode {
        return fraSisteMandagFør.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { mandag ->
            Periode(mandag, mandag.plusDays(13))
        }
    }
}
