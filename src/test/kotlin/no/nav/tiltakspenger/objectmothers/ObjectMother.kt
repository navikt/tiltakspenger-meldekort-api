package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object ObjectMother :
    MeldekortMother,
    MeldeperiodeMother,
    SakMother {
    /**
     * Lager en periode basert på siste mandag før [fraSisteMandagFør] eller eventuelt siste søndag etter [tilSisteSøndagEtter].
     */
    fun periode(
        fraSisteMandagFør: LocalDate = nå(fixedClock).toLocalDate(),
        tilSisteSøndagEtter: LocalDate? = null,
    ): Periode {
        if (tilSisteSøndagEtter != null) {
            return tilSisteSøndagEtter.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).let { søndag ->
                Periode(søndag.minusDays(13), søndag)
            }
        }

        return fraSisteMandagFør.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { mandag ->
            Periode(mandag, mandag.plusDays(13))
        }
    }

    const val FAKE_FNR = "12345678911"
}
