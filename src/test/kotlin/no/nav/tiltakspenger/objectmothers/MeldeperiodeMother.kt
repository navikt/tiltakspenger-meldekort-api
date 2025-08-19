package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.min

interface MeldeperiodeMother {
    fun meldeperiode(
        id: MeldeperiodeId = MeldeperiodeId.random(),
        periode: Periode = ObjectMother.periode(),
        kjedeId: MeldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
        saksnummer: String = Math.random().toString(),
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.fromString(FAKE_FNR),
        versjon: Int = 1,
        opprettet: LocalDateTime = nå(fixedClock),
        girRett: Map<LocalDate, Boolean> = periode.tilGirRett(),
        antallDagerForPeriode: Int = girRett.filter { it.value }.size,
    ): Meldeperiode {
        require(MeldeperiodeKjedeId.fraPeriode(periode) == kjedeId) {
            "KjedeId må være lik MeldeperiodeKjedeId.fraPeriode(periode)"
        }
        return Meldeperiode(
            id = id,
            periode = periode,
            saksnummer = saksnummer,
            sakId = sakId,
            fnr = fnr,
            kjedeId = kjedeId,
            versjon = versjon,
            opprettet = opprettet,
            maksAntallDagerForPeriode = antallDagerForPeriode,
            girRett = girRett,
        )
    }

    fun meldeperiodeDto(
        id: String = MeldeperiodeId.random().toString(),
        periode: Periode = ObjectMother.periode(),
        versjon: Int = 1,
        opprettet: LocalDateTime = nå(fixedClock),
        girRett: Map<LocalDate, Boolean> = periode.tilGirRett(),
        antallDagerForPeriode: Int = min(girRett.filter { it.value }.size, 10),
    ): SakTilMeldekortApiDTO.Meldeperiode {
        return SakTilMeldekortApiDTO.Meldeperiode(
            id = id,
            kjedeId = MeldeperiodeKjedeId.fraPeriode(periode).toString(),
            versjon = versjon,
            opprettet = opprettet,
            girRett = girRett,
            fraOgMed = periode.fraOgMed,
            tilOgMed = periode.tilOgMed,
            antallDagerForPeriode = antallDagerForPeriode,
        )
    }

    private fun Periode.tilGirRett(): Map<LocalDate, Boolean> = tilDager()
        .associateWith { value -> listOf(value.dayOfWeek).none { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY } }
}
