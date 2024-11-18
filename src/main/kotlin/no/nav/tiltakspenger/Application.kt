package no.nav.tiltakspenger

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging
import no.nav.tiltakspenger.Configuration.httpPort
import no.nav.tiltakspenger.routes.meldekortApi

fun main() {
    System.setProperty("logback.configurationFile", "logback.local.xml")

    val log = KotlinLogging.logger {}
    val securelog = KotlinLogging.logger("tjenestekall")

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        securelog.error(e) { e.message }
    }

    log.info { "starting server" }

    embeddedServer(
        factory = Netty,
        port = httpPort(),
        module = {
            meldekortApi(
                //applicationContext = applicationContext,
            )
        },
    ).start(wait = true)
}
