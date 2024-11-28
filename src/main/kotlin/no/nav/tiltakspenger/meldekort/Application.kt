package no.nav.tiltakspenger.meldekort

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KLogger
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.meldekort.Configuration.httpPort
import no.nav.tiltakspenger.meldekort.context.ApplicationContext
import no.nav.tiltakspenger.meldekort.routes.meldekortApi

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}

    start(log = log)
}

internal fun start(
    log: KLogger,
    port: Int = httpPort(),
    applicationContext: ApplicationContext = ApplicationContext(),
) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        sikkerlogg.error(e) { e.message }
    }

    log.info { "starting server" }

    embeddedServer(
        factory = Netty,
        port = port,
        module = {
            meldekortApi(applicationContext = applicationContext)
        },
    ).start(wait = true)
}
