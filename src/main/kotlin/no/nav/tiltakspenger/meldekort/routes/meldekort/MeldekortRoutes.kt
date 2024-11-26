package no.nav.tiltakspenger.meldekort.routes.meldekort

import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.tiltakspenger.meldekort.service.MeldekortService

internal fun Route.meldekortRoutes(
    meldekortService: MeldekortService,
) {
    post("/meldekort") {
        meldekortService.hentMeldekort("123")
        call.respondText("ok")
    }

    get("/test") {
        call.respondText("ok")
    }
}
