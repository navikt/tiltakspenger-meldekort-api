package no.nav.tiltakspenger.fakes.clients

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.meldekort.clients.microfrontend.TmsMikrofrontendClient

class TmsMikrofrontendClientFake : TmsMikrofrontendClient {
    private val logger = KotlinLogging.logger {}
    override fun aktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId) {
        logger.info { "Aktiverer (ikke) mikrofrontend for sakId=$sakId" }
    }

    override fun inaktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId) {
        logger.info { "Inaktiverer (ikke) mikrofrontend for sakId=$sakId" }
    }
}
