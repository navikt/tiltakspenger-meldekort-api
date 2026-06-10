package no.nav.tiltakspenger.meldekort.microfrontend

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr

class HentMeldekortInfoForMicrofrontendService(
    private val microfrontendRepo: MicrofrontendRepo,
) {
    fun hentInformasjonOmMeldekortForMicrofrontend(fnr: Fnr): Either<MicrofrontendFeil, MicrofrontendMeldekortInfo> {
        return microfrontendRepo.hentMeldekortInfo(fnr)
    }
}
