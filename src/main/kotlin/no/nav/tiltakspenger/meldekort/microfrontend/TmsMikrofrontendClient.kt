package no.nav.tiltakspenger.meldekort.microfrontend

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId

interface TmsMikrofrontendClient {
    fun aktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId): Either<MicrofrontendFeil, Unit>
    fun inaktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId): Either<MicrofrontendFeil, Unit>
}
