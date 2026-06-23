package no.nav.tiltakspenger.meldekort.infra.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.TexasAuthenticationProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.meldekort.bruker.infra.routes.brukerModule
import no.nav.tiltakspenger.meldekort.infra.ApplicationContext
import no.nav.tiltakspenger.meldekort.landingsside.infra.routes.landingssideModule
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.meldekortModule
import no.nav.tiltakspenger.meldekort.microfrontend.infra.routes.microfrontendModule
import no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottakModule
import org.slf4j.event.Level

const val CALL_ID_MDC_KEY = "call-id"

/**
 * Generisk Ktor-oppsett: plugins, autentisering og oppkobling av modulene per feature.
 *
 * Auth-provider og path-prefiks for hver feature eies av feature-pakken selv,
 * via `*Module(applicationContext)`-funksjonene.
 *
 * [additionalRoutes] er en hook for å registrere ekstra routes som ikke skal være med i prod
 * (f.eks. dev-only endepunkter satt opp fra LokalMain). Er `null` i prod.
 */
fun Application.ktorSetup(
    applicationContext: ApplicationContext,
    additionalRoutes: (Routing.() -> Unit)? = null,
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

        mottakModule(applicationContext)
        meldekortModule(applicationContext)
        brukerModule(applicationContext)
        microfrontendModule(applicationContext.hentMeldekortInfoForMicrofrontendService)
        landingssideModule(applicationContext)

        additionalRoutes?.invoke(this)
    }
}

fun Application.jacksonSerialization() {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
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
