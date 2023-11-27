package no.nav.tiltakspenger.meldekort.api

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.api.routes.healthRoutes
import no.nav.tiltakspenger.meldekort.api.routes.meldekort

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    val securelog = KotlinLogging.logger("tjenestekall")

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        securelog.error(e) { e.message }
    }

    log.info { "starting server" }

    embeddedServer(Netty, port = 8080, module = Application::applicationModule).start(wait = true)
}

fun Application.applicationModule() {
    routing {
        healthRoutes()
        meldekort()
    }
}
