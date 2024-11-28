package no.nav.tiltakspenger.meldekort.routes.meldekort

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.routes.getFnr
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

        call.respond(meldekort.tilUtfyllingDTO())
    }

    get("/meldekort/siste") {
        val fnr = call.getFnr()

        if (fnr == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        val meldekort = meldekortService.hentSisteMeldekort(fnr)
        if (meldekort == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        call.respond(meldekort.tilUtfyllingDTO())
    }

    get("/meldekort/alle") {
        val fnr = call.getFnr()

        if (fnr == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        val alleMeldekort = meldekortService.hentAlleMeldekort(fnr).map {
            it.tilUtfyllingDTO()
        }

        call.respond(alleMeldekort)
    }

    get("/meldekort/generer") {
        val fnr = call.getFnr()

        if (fnr == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@get
        }

        val meldekort = genererDummyMeldekort(fnr)

        meldekortService.lagreMeldekort(meldekort)

        call.respond(meldekort)
    }

    post("/meldekort/send-inn") {
        val body = try {
            deserialize<MeldekortFraUtfyllingDTO>(call.receiveText())
        } catch (e: Exception) {
            logger.error { "Error parsing body: $e" }
            null
        }

        if (body == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@post
        }

        meldekortService.oppdaterMeldekort(body)

        call.respond(HttpStatusCode.OK)
    }
}
