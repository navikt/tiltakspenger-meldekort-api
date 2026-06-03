package no.nav.tiltakspenger.objectmothers

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.meldekort.sak.Sak

interface SakMother {

    fun sak(
        id: SakId = SakId.random(),
        meldeperioder: List<Meldeperiode> = emptyList(),
        saksnummer: String = Math.random().toString(),
        fnr: Fnr = Fnr.random(),
        arenaMeldekortStatus: ArenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
        harSoknadUnderBehandling: Boolean = false,
        kanSendeInnHelgForMeldekort: Boolean = false,
    ): Sak {
        return Sak(
            id = id,
            saksnummer = saksnummer,
            fnr = fnr,
            meldeperioder = meldeperioder,
            arenaMeldekortStatus = arenaMeldekortStatus,
            harSoknadUnderBehandling = harSoknadUnderBehandling,
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
        )
    }

    fun sakDTO(
        sakId: String = SakId.random().toString(),
        saksnummer: String = Math.random().toString(),
        fnr: String = Fnr.random().verdi,
        meldeperioder: List<SakTilMeldekortApiDTO.MeldeperiodeDTO> = emptyList(),
        meldekortvedtak: List<SakTilMeldekortApiDTO.MeldekortvedtakDTO> = emptyList(),
        harSoknadUnderBehandling: Boolean = false,
        kanSendeInnHelgForMeldekort: Boolean = false,
    ): SakTilMeldekortApiDTO {
        return SakTilMeldekortApiDTO(
            fnr = fnr,
            sakId = sakId,
            saksnummer = saksnummer,
            meldeperioder = meldeperioder,
            harSoknadUnderBehandling = harSoknadUnderBehandling,
            kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort,
            meldekortvedtak = meldekortvedtak,
        )
    }
}
