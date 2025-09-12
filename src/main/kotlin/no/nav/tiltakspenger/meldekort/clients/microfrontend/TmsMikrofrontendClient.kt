package no.nav.tiltakspenger.meldekort.clients.microfrontend

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId

interface TmsMikrofrontendClient {
    fun aktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId)
    fun inaktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId)
}
