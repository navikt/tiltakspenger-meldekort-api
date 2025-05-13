package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.domene.MeldeperioderForSak
import no.nav.tiltakspenger.meldekort.domene.Sak
import no.nav.tiltakspenger.meldekort.service.SakDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR

interface SakMother {

    fun sak(
        id: SakId = SakId.random(),
        saksnummer: String = Math.random().toString(),
        fnr: Fnr = Fnr.fromString(FAKE_FNR),
        meldeperioder: List<Periode> = emptyList(),
        arenaMeldekortStatus: ArenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
    ): Sak {
        return Sak(
            id = id,
            saksnummer = saksnummer,
            fnr = fnr,
            meldeperioder = MeldeperioderForSak(meldeperioder),
            arenaMeldekortStatus = arenaMeldekortStatus,
        )
    }

    fun sakDTO(
        sakId: String = SakId.random().toString(),
        saksnummer: String = Math.random().toString(),
        fnr: String = FAKE_FNR,
        meldeperioder: List<PeriodeDTO> = emptyList(),
    ): SakDTO {
        return SakDTO(
            fnr = fnr,
            sakId = sakId,
            saksnummer = saksnummer,
            meldeperioder = meldeperioder,
        )
    }
}
