package no.nav.tiltakspenger.meldekort.microfrontend.infra.kafka

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.kafka.Producer
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendFeil
import no.nav.tiltakspenger.meldekort.microfrontend.TmsMikrofrontendClient
import no.nav.tms.microfrontend.MicrofrontendMessageBuilder.disable
import no.nav.tms.microfrontend.MicrofrontendMessageBuilder.enable
import no.nav.tms.microfrontend.Sensitivitet

const val MICROFRONTEND_APP: String = "tiltakspenger-meldekort-microfrontend"
const val MICROFRONTEND_TEAM: String = "tpts"

/**
 * Kafka-implementasjon av [TmsMikrofrontendClient].
 * Publiserer enable-/disable-meldinger på microfrontend-topic-en med sakId som key.
 *
 * [produserMelding] er en søm slik at meldingsbyggingen kan testes med en ekte fake i stedet for en mock – i produksjon settes den til [Producer.produce].
 * [Producer] er en final-klasse rundt KafkaProducer og kan ikke fakes direkte.
 *
 * Se: https://tms-docs.nav.no/microfrontends/microfrontend-ssr/ og https://navikt.github.io/tms-dokumentasjon/microfrontend/ og https://aksel.nav.no/god-praksis/artikler/retningslinjer-for-design-av-mikrofrontends?tema=design
 * Slack: #minside-microfrontends (eller #team-personbruker)
 */
class TmsMikrofrontendKafkaProducer(
    private val topicName: String,
    private val produserMelding: (topic: String, key: String, value: String) -> Unit,
) : TmsMikrofrontendClient {

    override fun aktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId): Either<MicrofrontendFeil, Unit> = Either.catch {
        val aktiverMelding = enable(
            ident = fnr.verdi,
            microfrontendId = MICROFRONTEND_APP,
            initiatedBy = MICROFRONTEND_TEAM,
            sensitivitet = Sensitivitet.SUBSTANTIAL,
        )

        produserMelding(topicName, sakId.toString(), aktiverMelding.text())
    }.mapLeft { MicrofrontendFeil.KafkaFeil(it) }

    override fun inaktiverMicrofrontendForBruker(fnr: Fnr, sakId: SakId): Either<MicrofrontendFeil, Unit> = Either.catch {
        val inaktiverMelding = disable(
            ident = fnr.verdi,
            microfrontenId = MICROFRONTEND_APP,
            initiatedBy = MICROFRONTEND_TEAM,
        )

        produserMelding(topicName, sakId.toString(), inaktiverMelding.text())
    }.mapLeft { MicrofrontendFeil.KafkaFeil(it) }
}
