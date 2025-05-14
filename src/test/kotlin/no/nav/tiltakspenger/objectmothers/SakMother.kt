package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.Sak
import no.nav.tiltakspenger.meldekort.routes.meldekort.saksbehandling.SakDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR

interface SakMother {

    fun sak(
        id: SakId = SakId.random(),
        saksnummer: String = Math.random().toString(),
        fnr: Fnr = Fnr.fromString(FAKE_FNR),
        meldeperioder: List<Meldeperiode> = emptyList(),
        arenaMeldekortStatus: ArenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
    ): Sak {
        return Sak(
            id = id,
            saksnummer = saksnummer,
            fnr = fnr,
            meldeperioder = meldeperioder,
            arenaMeldekortStatus = arenaMeldekortStatus,
        )
    }

    fun sakDTO(
        sakId: String = SakId.random().toString(),
        saksnummer: String = Math.random().toString(),
        fnr: String = FAKE_FNR,
        meldeperioder: List<SakDTO.MeldeperiodeDTO> = emptyList(),
    ): SakDTO {
        return SakDTO(
            fnr = fnr,
            sakId = sakId,
            saksnummer = saksnummer,
            meldeperioder = meldeperioder,
        )
    }
}
