package no.nav.tiltakspenger.meldekort.routes

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.routing.routing
import no.nav.security.token.support.v2.asIssuerProps
import no.nav.tiltakspenger.meldekort.auth.TexasWall
import no.nav.tiltakspenger.meldekort.auth.getSecurityConfig
import no.nav.tiltakspenger.meldekort.auth.installAuthentication
import no.nav.tiltakspenger.meldekort.context.ApplicationContext
import no.nav.tiltakspenger.meldekort.routes.meldekort.meldekortRoutes
import org.slf4j.event.Level

internal fun Application.meldekortApi(
    applicationContext: ApplicationContext,
) {
    install(CallLogging) {
        level = Level.INFO
        filter { call ->
            !call.request.path().startsWith("/isalive") &&
                    !call.request.path().startsWith("/isready") &&
                    !call.request.path().startsWith("/metrics")
        }
    }
    jacksonSerialization()

    routing {
        healthRoutes()

        meldekortRoutes(
            meldekortService = applicationContext.meldekortService,
            texasHttpClient = applicationContext.texasHttpClient
        )
    }
}

fun Application.jacksonSerialization() {
    install(ContentNegotiation) {
        jackson {
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
        }
    }
}
