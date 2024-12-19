package no.nav.tiltakspenger.meldekort.routes.meldekort

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.auth.TexasWallBrukerToken
import no.nav.tiltakspenger.meldekort.auth.TexasWallSystemToken
import no.nav.tiltakspenger.meldekort.auth.fnrAttributeKey
import no.nav.tiltakspenger.meldekort.clients.TexasHttpClient
import no.nav.tiltakspenger.meldekort.domene.genererDummyMeldekort
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortFraUtfylling
import no.nav.tiltakspenger.meldekort.service.MeldekortService

val logger = KotlinLogging.logger {}

internal fun Route.meldekortRoutes(
    meldekortService: MeldekortService,
    texasHttpClient: TexasHttpClient,
) {
    // Kalles fra saksbehandling-api
    route("/meldekort", HttpMethod.Post) {
        install(TexasWallSystemToken) {
            client = texasHttpClient
        }

        handle {
            val meldekortFraSaksbehandling = try {
                deserialize<MeldekortTilBrukerDTO>(call.receiveText())
            } catch (e: Exception) {
                logger.error { "Error parsing body: $e" }
                null
            }

            if (meldekortFraSaksbehandling == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@handle
            }

            val meldekort = meldekortFraSaksbehandling.tilMeldekort()

            if (meldekortService.hentMeldekort(meldekort.id) != null) {
                call.respond(message = "Meldekortet finnes allerede", status = HttpStatusCode.Conflict)
                return@handle
            }

            meldekortService.lagreMeldekort(meldekort).onLeft {
                call.respond(message = "Lagring av meldekortet feilet", status = HttpStatusCode.InternalServerError)
            }.onRight {
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    // Endepunkter som kalles fra brukers meldekort-frontend
    route("/meldekort/bruker") {
        install(TexasWallBrukerToken) {
            client = texasHttpClient
        }

        get("{meldekortId}") {
            val meldekortIdParam = call.parameters["meldekortId"]
            if (meldekortIdParam == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val meldekortId = MeldekortId.Companion.fromString(meldekortIdParam)

            val meldekort = meldekortService.hentMeldekort(meldekortId)
            if (meldekort == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respond(meldekort.tilUtfyllingDTO())
        }

        get("siste") {
            val fnr = call.attributes[fnrAttributeKey]

            val meldekort = meldekortService.hentSisteMeldekort(fnr)
            if (meldekort == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respond(meldekort.tilUtfyllingDTO())
        }

        get("alle") {
            val fnr = call.attributes[fnrAttributeKey]

            val alleMeldekort = meldekortService.hentAlleMeldekort(fnr).map {
                it.tilUtfyllingDTO()
            }

            call.respond(alleMeldekort)
        }

        get("generer") {
            val fnr = call.attributes[fnrAttributeKey]

            val meldekort = genererDummyMeldekort(fnr)

            meldekortService.lagreMeldekort(meldekort)

            call.respond(meldekort)
        }

        post("send-inn") {
            val meldekortFraUtfyllingDTO = try {
                deserialize<MeldekortFraUtfyllingDTO>(call.receiveText())
            } catch (e: Exception) {
                logger.error { "Error parsing body: $e" }
                null
            }

            if (meldekortFraUtfyllingDTO == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            meldekortService.oppdaterMeldekort(meldekortFraUtfyllingDTO.tilMeldekortFraUtfylling())

            call.respond(HttpStatusCode.OK)
        }
    }
}
