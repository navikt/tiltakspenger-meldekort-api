package no.nav.tiltakspenger.routes.meldekort

import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

internal fun Route.meldekortRoutes() {
    post("/meldekort") {
        call.respondText("ok")
    }

    get("/test") {
        call.respondText("ok")
    }
}
