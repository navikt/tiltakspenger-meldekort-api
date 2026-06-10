package no.nav.tiltakspenger.meldekort.microfrontend

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

interface MicrofrontendRepo {
    fun oppdaterStatusForMicrofrontend(
        sakId: SakId,
        aktiv: Boolean,
        sessionContext: SessionContext? = null,
    ): Either<MicrofrontendFeil, Unit>

    fun hentSakerHvorMicrofrontendSkalAktiveres(limit: Int = 100, sessionContext: SessionContext? = null): Either<MicrofrontendFeil, List<MicrofrontendSak>>
    fun hentSakerHvorMicrofrontendSkalInaktiveres(limit: Int = 100, sessionContext: SessionContext? = null): Either<MicrofrontendFeil, List<MicrofrontendSak>>

    fun hentMeldekortInfo(fnr: Fnr, sessionContext: SessionContext? = null): Either<MicrofrontendFeil, MicrofrontendMeldekortInfo>
}
