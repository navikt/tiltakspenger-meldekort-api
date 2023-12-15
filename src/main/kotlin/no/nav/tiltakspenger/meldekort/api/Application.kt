package no.nav.tiltakspenger.meldekort.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.api.db.flywayMigrate
import no.nav.tiltakspenger.meldekort.api.repository.GrunnlagRepoImpl
import no.nav.tiltakspenger.meldekort.api.repository.GrunnlagTiltakRepo
import no.nav.tiltakspenger.meldekort.api.repository.MeldekortRepoImpl
import no.nav.tiltakspenger.meldekort.api.routes.healthRoutes
import no.nav.tiltakspenger.meldekort.api.routes.meldekort
import no.nav.tiltakspenger.meldekort.api.service.MeldekortServiceImpl

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
    val meldekortRepo = MeldekortRepoImpl()
    val grunnlagTiltakRepo = GrunnlagTiltakRepo()
    val grunnlagRepo = GrunnlagRepoImpl(grunnlagTiltakRepo)
    val meldekortService = MeldekortServiceImpl(meldekortRepo, grunnlagRepo)

    installJacksonFeature()
    flywayMigrate()
    routing {
        healthRoutes()
        meldekort(meldekortService)
    }
}

fun Application.installJacksonFeature() {
    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
        }
    }
}
