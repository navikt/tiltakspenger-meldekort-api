package no.nav.tiltakspenger.meldekort.clients.microfrontend

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tms.microfrontend.MicrofrontendMessageBuilder.disable
import no.nav.tms.microfrontend.MicrofrontendMessageBuilder.enable
import no.nav.tms.microfrontend.Sensitivitet

const val MICROFRONTEND_APP: String = "tiltakspenger-meldekort-microfrontend"
const val MICROFRONTEND_TEAM: String = "tpts"

class TmsMikrofrontendClientImpl(
    val kafkaProducer: Producer<String, String>,
    val topicName: String,
) : TmsMikrofrontendClient {
    private val log = KotlinLogging.logger { }

    override fun aktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId) {
        log.info { "Aktiverer microfrontend for bruker med sakId=$sakId" }
        val aktiverMelding = enable(
            ident = fnr.verdi,
            microfrontendId = MICROFRONTEND_APP,
            initiatedBy = MICROFRONTEND_TEAM,
            sensitivitet = Sensitivitet.HIGH,
        )

        kafkaProducer.produce(topicName, sakId.toString(), aktiverMelding.text())
    }

    override fun inaktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId) {
        log.info { "Inaktiverer microfrontend for bruker med sakId=$sakId" }
        val inaktiverMelding = disable(
            ident = fnr.verdi,
            microfrontenId = MICROFRONTEND_APP,
            initiatedBy = MICROFRONTEND_TEAM,
        )

        kafkaProducer.produce(topicName, sakId.toString(), inaktiverMelding.text())
    }
}
