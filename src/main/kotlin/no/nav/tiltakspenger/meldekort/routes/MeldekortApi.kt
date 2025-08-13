package no.nav.tiltakspenger.meldekort.routes

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.server.routing.routing
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.TexasAuthenticationProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.meldekort.context.ApplicationContext
import no.nav.tiltakspenger.meldekort.routes.meldekort.meldekortRoutes
import org.slf4j.event.Level

const val CALL_ID_MDC_KEY = "call-id"

fun Application.meldekortApi(
    applicationContext: ApplicationContext,
) {
    install(CallId) {
        generate { java.util.UUID.randomUUID().toString() }
        retrieve { call -> call.request.header(HttpHeaders.XRequestId) }
        verify { callId: String -> callId.isNotEmpty() }
    }
    install(CallLogging) {
        callIdMdc(CALL_ID_MDC_KEY)
        level = Level.INFO
        filter { call ->
            !call.request.path().startsWith("/isalive") &&
                !call.request.path().startsWith("/isready") &&
                !call.request.path().startsWith("/metrics")
        }
    }
    jacksonSerialization()

    setupAuthentication(applicationContext.texasClient)

    routing {
        healthRoutes()

        meldekortRoutes(
            meldekortService = applicationContext.meldekortService,
            brukerService = applicationContext.brukerService,
            lagreFraSaksbehandlingService = applicationContext.lagreFraSaksbehandlingService,
            clock = applicationContext.clock,
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

fun Application.setupAuthentication(texasClient: TexasClient) {
    authentication {
        register(
            TexasAuthenticationProvider(
                TexasAuthenticationProvider.Config(
                    name = IdentityProvider.TOKENX.value,
                    texasClient = texasClient,
                    identityProvider = IdentityProvider.TOKENX,
                    requireIdportenLevelHigh = false,
                ),
            ),
        )

        register(
            TexasAuthenticationProvider(
                TexasAuthenticationProvider.Config(
                    name = IdentityProvider.AZUREAD.value,
                    texasClient = texasClient,
                    identityProvider = IdentityProvider.AZUREAD,
                ),
            ),
        )
    }
}
