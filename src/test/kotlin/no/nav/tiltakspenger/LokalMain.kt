package no.nav.tiltakspenger

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.meldekort.start

/**
 * Starter opp serveren lokalt med postgres og auth i docker og in-memory fakes.
 * Gjenbruker lokale innstillinger i [Configuration]
 * Gjenbruker fakes fra testene.
 * Dette er et alternativ til å starte opp serveren med docker-compose (som bruker wiremock for eksterne tjenester).
 */
fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    log.info { "Starter lokal server. Bruker default postgres i docker og in-memory fakes." }
    start(
        log = log,
        isNais = false,
        applicationContext = TestApplicationContext(),
    )
}
