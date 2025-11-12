package no.nav.tiltakspenger

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.periodisering.zoneIdOslo
import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.meldekort.start
import java.time.Clock

/**
 * Starter opp serveren lokalt med postgres i docker og fakes fra [LokalApplicationContext]
 */
fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    log.info { "Starter lokal server" }
    start(
        log = log,
        isNais = false,
        applicationContext = LokalApplicationContext(Clock.system(zoneIdOslo)),
    )
}
