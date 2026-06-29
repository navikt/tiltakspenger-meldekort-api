package no.nav.tiltakspenger.meldekort.infra

import no.nav.tiltakspenger.libs.ktor.common.oppstart.KafkaConsumerOppsett

/**
 * Kafka-consumerene som skal startes ved oppstart og stoppes ved shutdown.
 * Identhendelse-consumeren kjører kun i NAIS (lokalt kjører vi ikke mot Kafka).
 */
fun kafkaConsumers(
    isNais: Boolean,
    applicationContext: ApplicationContext,
): List<KafkaConsumerOppsett> = if (isNais) {
    listOf(
        KafkaConsumerOppsett(
            navn = "kafka-consumer-identhendelse",
            // run() er ikke-blokkerende; den starter konsument-loopen på Dispatchers.IO og returnerer umiddelbart.
            start = { applicationContext.identhendelseConsumer.run() },
            stopp = { applicationContext.identhendelseConsumer.stop() },
        ),
    )
} else {
    emptyList()
}
