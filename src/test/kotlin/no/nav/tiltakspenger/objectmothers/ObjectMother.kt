package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

object ObjectMother :
    MeldekortMother,
    MeldeperiodeMother {
    fun periode(): Periode {
        return nå().toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { mandag ->
            Periode(mandag, mandag.plusDays(13))
        }
    }
}
