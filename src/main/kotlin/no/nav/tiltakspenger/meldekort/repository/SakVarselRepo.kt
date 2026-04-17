package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Sak
import java.time.LocalDateTime

interface SakVarselRepo {

    fun flaggForVarselvurdering(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    )

    fun hentSakerSomSkalVurdereVarsel(
        limit: Int = 25,
        sessionContext: SessionContext? = null,
    ): List<Sak>

    fun markerVarselVurdert(
        sakId: SakId,
        vurdertTidspunkt: LocalDateTime,
        sessionContext: SessionContext? = null,
    )
}
