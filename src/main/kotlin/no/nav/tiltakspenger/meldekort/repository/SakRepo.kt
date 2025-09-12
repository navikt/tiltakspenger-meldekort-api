package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.domene.Sak
import java.time.Clock

interface SakRepo {
    fun lagre(
        sak: Sak,
        sessionContext: SessionContext? = null,
    )

    fun oppdater(
        sak: Sak,
        sessionContext: SessionContext? = null,
    )

    fun oppdaterStatusForMicrofrontend(
        sakId: SakId,
        aktiv: Boolean,
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

    fun hentTilBruker(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): Sak?

    fun hentSakerUtenArenaStatus(sessionContext: SessionContext? = null): List<Sak>
    fun hentSakerHvorMicrofrontendSkalAktiveres(sessionContext: SessionContext? = null, clock: Clock): List<Sak>
    fun hentSakerHvorMicrofrontendSkalInaktiveres(sessionContext: SessionContext? = null, clock: Clock): List<Sak>
}
