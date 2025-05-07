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
        TODO("Not yet implemented")
    }

    override fun oppdaterArenaStatus(
        id: SakId,
        arenaStatus: ArenaMeldekortStatus,
        sessionContext: SessionContext?,
    ) {
        TODO("Not yet implemented")
    }

    override fun hent(
        id: SakId,
        sessionContext: SessionContext?,
    ): Sak? {
        TODO("Not yet implemented")
    }

    override fun hent(
        fnr: Fnr,
        sessionContext: SessionContext?,
    ): Sak? {
        TODO("Not yet implemented")
    }

    override fun hentSakerUtenArenaStatus(sessionContext: SessionContext?): List<Sak> {
        TODO("Not yet implemented")
    }
}
