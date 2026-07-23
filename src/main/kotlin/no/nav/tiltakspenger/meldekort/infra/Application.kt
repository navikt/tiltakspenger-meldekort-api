package no.nav.tiltakspenger.meldekort.infra

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Bakgrunnsprosessoppsett
import no.nav.tiltakspenger.libs.ktor.common.oppstart.startApp
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.meldekort.infra.Configuration.httpPort
import no.nav.tiltakspenger.meldekort.infra.routes.CALL_ID_MDC_KEY
import no.nav.tiltakspenger.meldekort.infra.routes.ktorSetup
import java.time.Clock

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}

    start(log = log)
}

fun start(
    log: KLogger,
    port: Int = httpPort(),
    host: String = "0.0.0.0",
    isNais: Boolean = Configuration.isNais(),
    applicationContext: ApplicationContext = ApplicationContext(
        clock = Clock.system(zoneIdOslo),
    ),
    additionalRoutes: (io.ktor.server.routing.Routing.() -> Unit)? = null,
) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e.message }
    }

    startApp(
        log = log,
        port = port,
        host = host,
        isNais = isNais,
        oppsett = Bakgrunnsprosessoppsett(
            mdcCallIdKey = CALL_ID_MDC_KEY,
            electorPath = Configuration::electorPath,
            tasks = jobber(applicationContext),
            kafkaConsumers = kafkaConsumers(isNais = isNais, applicationContext = applicationContext),
            clock = applicationContext.clock,
        ),
    ) { readiness ->
        ktorSetup(applicationContext = applicationContext, readiness = readiness, additionalRoutes = additionalRoutes)
    }
}
