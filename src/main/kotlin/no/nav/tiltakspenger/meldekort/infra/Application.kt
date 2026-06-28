package no.nav.tiltakspenger.meldekort.infra

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Readiness
import no.nav.tiltakspenger.libs.ktor.common.oppstart.startKtorServer
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.meldekort.infra.Configuration.httpPort
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
    isNais: Boolean = Configuration.isNais(),
    applicationContext: ApplicationContext = ApplicationContext(
        clock = Clock.system(zoneIdOslo),
    ),
    additionalRoutes: (io.ktor.server.routing.Routing.() -> Unit)? = null,
) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e.message }
    }

    val readiness = Readiness()

    startKtorServer(log = log, port = port) { shutdownPågår ->
        konfigurerMeldekortLivssyklus(
            log = log,
            isNais = isNais,
            applicationContext = applicationContext,
            readiness = readiness,
            shutdownPågår = shutdownPågår,
        )
        ktorSetup(applicationContext = applicationContext, readiness = readiness, additionalRoutes = additionalRoutes)
    }
}
