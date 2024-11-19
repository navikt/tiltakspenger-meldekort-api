package no.nav.tiltakspenger.routes.meldekort

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

private val logger = KotlinLogging.logger {}

internal fun Route.meldekortRoutes() {
    post("/meldekort") {
            logger.info { "Henter meldekort" }
    }
}