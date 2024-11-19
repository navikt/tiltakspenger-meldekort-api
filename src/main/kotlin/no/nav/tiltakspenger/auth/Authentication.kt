package no.nav.tiltakspenger.auth

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.config.ApplicationConfig
import no.nav.security.token.support.v2.tokenValidationSupport

fun Application.initAuthentication() {
    val isNaisEnv = System.getenv("NAIS_CLUSTER_NAME") != null

    val configFile = if (isNaisEnv) "security.conf" else "security.local.conf"

    install(Authentication) {
        tokenValidationSupport(
            config = ApplicationConfig(configFile),
        )
    }
}
