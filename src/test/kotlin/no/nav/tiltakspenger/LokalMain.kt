package no.nav.tiltakspenger

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.dev.devRoutes
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.meldekort.infra.Configuration
import no.nav.tiltakspenger.meldekort.infra.start
import java.time.Clock

/**
 * Starter opp serveren lokalt med postgres i docker og fakes fra [LokalApplicationContext]
 */
fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    log.info { "Starter lokal server" }
    val applicationContext = LokalApplicationContext(Clock.system(zoneIdOslo))
    start(
        log = log,
        isNais = false,
        applicationContext = applicationContext,
        // Dev-only endepunkter (f.eks. POST /dev/sak). Aldri med i prod.
        additionalRoutes = { devRoutes(applicationContext) },
    )
}
