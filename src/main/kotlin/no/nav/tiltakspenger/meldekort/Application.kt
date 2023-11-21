package no.nav.tiltakspenger.meldekort

import mu.KotlinLogging
import io.ktor.server.routing.routing
import no.nav.tiltakspenger.meldekort.routes.meldekort
import no.nav.tiltakspenger.meldekort.service.MeldekortServiceImpl

fun main() {
    System.setProperty("logback.configurationFile", "egenLogback.xml")

    val log = KotlinLogging.logger {}
    val securelog = KotlinLogging.logger("tjenestekall")

    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        securelog.error(e) { e.message }
    }

    log.info { "starting server" }
}

fun io.ktor.server.application.Application.module() {
    val meldekortService = MeldekortServiceImpl()

    routing {
        //healthRoutes()
        meldekort()
    }
}
