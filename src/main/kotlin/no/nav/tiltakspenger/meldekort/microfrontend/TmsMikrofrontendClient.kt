package no.nav.tiltakspenger.meldekort.microfrontend

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr

interface TmsMikrofrontendClient {
    fun aktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId): Either<MicrofrontendFeil, Unit>
    fun inaktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId): Either<MicrofrontendFeil, Unit>
}
