package no.nav.tiltakspenger.objectmothers

import kotlinx.datetime.DayOfWeek
import no.nav.tiltakspenger.fakes.TEXAS_FAKE_FNR
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode

interface MeldeperiodeMother {
    fun meldeperiode(
        periode: Periode = ObjectMother.periode(),
        saksnummer: String? = Math.random().toString(),
        fnr: Fnr = Fnr.fromString(TEXAS_FAKE_FNR),
    ): Meldeperiode {
        return Meldeperiode(
            id = MeldeperiodeId.random(),
            kjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
            versjon = 1,
            sakId = SakId.random(),
            saksnummer = saksnummer,
            fnr = fnr,
            periode = periode,
            opprettet = nå(),
            maksAntallDagerForPeriode = periode.antallDager.toInt(),
            girRett = periode.tilDager().associateWith { value -> listOf(value.dayOfWeek).none { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY } },
        )
    }
}
