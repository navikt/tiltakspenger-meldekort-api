package no.nav.tiltakspenger.meldekort.landingsside

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

interface LandingssideRepo {
    fun hentSak(
        fnr: Fnr,
        sessionContext: SessionContext? = null,
    ): LandingssideSak?
}
