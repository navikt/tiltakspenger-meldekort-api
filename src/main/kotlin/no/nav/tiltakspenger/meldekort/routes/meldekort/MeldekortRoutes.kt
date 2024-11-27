package no.nav.tiltakspenger.meldekort.routes.meldekort

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.domene.genererDummyMeldekort
import no.nav.tiltakspenger.meldekort.service.MeldekortService

val logger = KotlinLogging.logger {}

internal fun Route.meldekortRoutes(
    meldekortService: MeldekortService,
) {
    get("/meldekort/{meldekortId}") {
        // TODO kew: midlertidig frem til vi får på plass den fra ApplicationCallEx.kt
        val meldekortId = call.parameters["meldekortId"]?.let { id -> MeldekortId.Companion.fromString(id) }

        if (meldekortId == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        val meldekort = meldekortService.hentMeldekort(meldekortId)
        if (meldekort == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        call.respond(meldekort)
    }

    get("/meldekort/siste") {
        // TODO kew: midlertidig frem til vi får på plass den fra ApplicationCallEx.kt
        val fnr = call.request.queryParameters["fnr"]?.let { fnr -> Fnr.fromString(fnr) }

        if (fnr == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        val meldekort = meldekortService.hentSisteMeldekort(fnr)
        if (meldekort == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        call.respond(meldekort)
    }

    get("/meldekort/alle") {
        // TODO kew: midlertidig frem til vi får på plass den fra ApplicationCallEx.kt
        val fnr = call.request.queryParameters["fnr"]?.let { fnr -> Fnr.fromString(fnr) }

        if (fnr == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        val alleMeldekort = meldekortService.hentAlleMeldekort(fnr)

        call.respond(alleMeldekort)
    }

    get("/meldekort/generer") {
        // TODO kew: midlertidig frem til vi får på plass den fra ApplicationCallEx.kt
        val fnr = call.request.queryParameters["fnr"]?.let { fnr -> Fnr.fromString(fnr) }

        if (fnr == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        val meldekort = genererDummyMeldekort(fnr)

        meldekortService.lagreMeldekort(meldekort)

        call.respond(meldekort)
    }
}
