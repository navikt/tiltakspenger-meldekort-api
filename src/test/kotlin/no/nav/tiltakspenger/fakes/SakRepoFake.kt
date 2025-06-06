package no.nav.tiltakspenger.fakes

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.domene.Sak
import no.nav.tiltakspenger.meldekort.repository.SakRepo

class SakRepoFake : SakRepo {
    private val data = Atomic(mutableMapOf<SakId, Sak>())

    override fun lagre(
        sak: Sak,
        sessionContext: SessionContext?,
    ) {
        data.get()[sak.id] = sak
    }

    override fun oppdater(
        sak: Sak,
        sessionContext: SessionContext?,
    ) {
        data.get()[sak.id] = data.get()[sak.id]!!.copy(
            fnr = sak.fnr,
            meldeperioder = sak.meldeperioder,
            harSoknadUnderBehandling = sak.harSoknadUnderBehandling,
        )
    }

    override fun oppdaterArenaStatus(
        id: SakId,
        arenaStatus: ArenaMeldekortStatus,
        sessionContext: SessionContext?,
    ) {
        data.get()[id] = data.get()[id]!!.copy(
            arenaMeldekortStatus = arenaStatus,
        )
    }

    override fun hent(
        id: SakId,
        sessionContext: SessionContext?,
    ): Sak? {
        return data.get()[id]
    }

    override fun hentTilBruker(
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): Sak? {
        return data.get().values.find { it.fnr == fnr }
    }

    override fun hentSakerUtenArenaStatus(sessionContext: SessionContext?): List<Sak> {
        return data.get().values.filter { it.arenaMeldekortStatus == ArenaMeldekortStatus.UKJENT }
    }
}
