package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Sak

interface SakRepo {
    fun lagre(
        sak: Sak,
        sessionContext: SessionContext? = null,
    )

    fun oppdater(
        sak: Sak,
        sessionContext: SessionContext? = null,
    )

    fun hent(
        id: SakId,
        sessionContext: SessionContext? = null,
    ): Sak?
}
