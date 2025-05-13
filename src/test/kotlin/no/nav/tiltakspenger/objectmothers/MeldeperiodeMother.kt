package no.nav.tiltakspenger.objectmothers

import kotlinx.datetime.DayOfWeek
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.meldekort.domene.DAGER_FØR_PERIODE_SLUTT_FOR_INNSENDING
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.NesteMeldeperiodeDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR
import java.time.LocalDate
import java.time.LocalDateTime

interface MeldeperiodeMother {
    fun meldeperiode(
        id: MeldeperiodeId = MeldeperiodeId.random(),
        periode: Periode = ObjectMother.periode(),
        saksnummer: String = Math.random().toString(),
        sakId: SakId = SakId.random(),
        fnr: Fnr = Fnr.fromString(FAKE_FNR),
        versjon: Int = 1,
        opprettet: LocalDateTime = nå(fixedClock),
        girRett: Map<LocalDate, Boolean> = periode.tilGirRett(),
    ): Meldeperiode {
        return Meldeperiode(
            id = id,
            periode = periode,
            saksnummer = saksnummer,
            sakId = sakId,
            fnr = fnr,
            kjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
            versjon = versjon,
            opprettet = opprettet,
            maksAntallDagerForPeriode = periode.antallDager.toInt(),
            girRett = girRett,
        )
    }

    fun meldeperiodeDto(
        id: String = MeldeperiodeId.random().toString(),
        periode: Periode = ObjectMother.periode(),
        saksnummer: String = Math.random().toString(),
        sakId: String = SakId.random().toString(),
        fnr: String = FAKE_FNR,
        versjon: Int = 1,
        opprettet: LocalDateTime = nå(fixedClock),
        girRett: Map<LocalDate, Boolean> = periode.tilGirRett(),
    ): MeldeperiodeDTO {
        return MeldeperiodeDTO(
            id = id,
            kjedeId = MeldeperiodeKjedeId.fraPeriode(periode).toString(),
            saksnummer = saksnummer,
            sakId = sakId,
            fnr = fnr,
            versjon = versjon,
            opprettet = opprettet,
            girRett = girRett,
            fraOgMed = periode.fraOgMed,
            tilOgMed = periode.tilOgMed,
            antallDagerForPeriode = periode.antallDager.toInt(),
        )
    }

    fun nesteMeldeperiodeDTO(periode: Periode): NesteMeldeperiodeDTO {
        return NesteMeldeperiodeDTO(
            kanSendes = periode.tilOgMed.minusDays(DAGER_FØR_PERIODE_SLUTT_FOR_INNSENDING),
            periode = periode.toDTO(),
        )
    }

    private fun Periode.tilGirRett(): Map<LocalDate, Boolean> = tilDager()
        .associateWith { value -> listOf(value.dayOfWeek).none { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY } }
}
