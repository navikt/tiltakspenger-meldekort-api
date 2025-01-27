package no.nav.tiltakspenger.meldekort.routes

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.authorization

fun ApplicationCall.bearerToken(): String? =
    request.authorization()
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.removePrefix("Bearer ")
