package no.nav.tiltakspenger.meldekort.api.service

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import no.nav.tiltakspenger.meldekort.api.Configuration.tokenValidationConfigAzure
import no.nav.tiltakspenger.meldekort.api.installJacksonFeature
import no.nav.tiltakspenger.meldekort.api.installTokenValidation
import no.nav.tiltakspenger.meldekort.api.routes.healthRoutes
import no.nav.tiltakspenger.meldekort.api.routes.meldekort
import no.nav.tiltakspenger.meldekort.api.tilgang.InnloggetBrukerProvider

fun ApplicationTestBuilder.testApplikasjon(
    port: Int = 8080,
    meldekortService: MeldekortService = mockk(),
    innloggetBrukerProvider: InnloggetBrukerProvider = InnloggetBrukerProvider(),
) {
    application {
        installJacksonFeature()
        installTokenValidation(
            tokenValidationConfigAzure(
                wellknownUrl = "http://localhost:$port/azure/.well-known/openid-configuration",
                clientId = "validAudience",
            ),
        )
        routing {
            healthRoutes()
            authenticate("azure") {
                meldekort(
                    meldekortService,
                    innloggetBrukerProvider,
                )
            }
        }
    }
}
