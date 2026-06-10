package no.nav.tiltakspenger.meldekort.sak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus

interface SakRepo {

    fun oppdaterFnr(
        gammeltFnr: Fnr,
        nyttFnr: Fnr,
        sessionContext: SessionContext? = null,
    )

    fun oppdaterArenaStatus(
        id: SakId,
        arenaStatus: ArenaMeldekortStatus,
        sessionContext: SessionContext? = null,
    )

    fun hent(
        id: SakId,
        sessionContext: SessionContext? = null,
    ): Sak?

    fun hentSakerUtenArenaStatus(sessionContext: SessionContext? = null): List<Sak>
}
