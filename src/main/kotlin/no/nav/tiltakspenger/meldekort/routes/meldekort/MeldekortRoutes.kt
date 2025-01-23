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
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.auth.TexasWallBrukerToken
import no.nav.tiltakspenger.meldekort.auth.TexasWallSystemToken
import no.nav.tiltakspenger.meldekort.auth.fnrAttributeKey
import no.nav.tiltakspenger.meldekort.clients.TexasHttpClient
import no.nav.tiltakspenger.meldekort.service.BrukersMeldekortService
import no.nav.tiltakspenger.meldekort.service.MeldeperiodeService

val logger = KotlinLogging.logger {}

internal fun Route.meldekortRoutes(
    brukersMeldekortService: BrukersMeldekortService,
    meldeperiodeService: MeldeperiodeService,
    texasHttpClient: TexasHttpClient,
) {
    // Kalles fra saksbehandling-api (sender meldeperiodene til meldekort-api)
    route("/meldekort", HttpMethod.Post) {
        install(TexasWallSystemToken) {
            client = texasHttpClient
        }

        handle {
            val meldeperiode = try {
                deserialize<MeldeperiodeDTO>(call.receiveText())
            } catch (e: Exception) {
                logger.error { "Error parsing body: $e" }
                null
            }

            if (meldeperiode == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@handle
            }

            val meldekort = meldeperiode.tilMeldeperiode()

            if (meldeperiodeService.hentMeldeperiodeForId(meldekort.id) != null) {
                call.respond(message = "Meldekortet finnes allerede", status = HttpStatusCode.Conflict)
                return@handle
            }

            meldeperiodeService.lagreMeldeperiode(meldekort).onLeft {
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

        get("{meldeperiodeKjedeId}") {
            val meldeperiodeKjedeIdParam = call.parameters["meldeperiodeKjedeId"]
            if (meldeperiodeKjedeIdParam == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            brukersMeldekortService.hentMeldekortForMeldeperiodeKjedeId(meldeperiodeKjedeIdParam)?.also {
                call.respond(it.tilUtfyllingDTO())
                return@get
            }
            meldeperiodeService.hentMeldeperiodeForKjedeId(meldeperiodeKjedeIdParam)?.also {
                call.respond(it.tilUtfyllingDTO())
                return@get
            }
            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        get("siste") {
            val fnr = call.attributes[fnrAttributeKey]

            val meldekort = brukersMeldekortService.hentSisteMeldekort(fnr)
            if (meldekort == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respond(meldekort.tilUtfyllingDTO())
        }

        get("alle") {
            val fnr = call.attributes[fnrAttributeKey]

            val alleMeldekort = brukersMeldekortService.hentAlleMeldekort(fnr).map {
                it.tilUtfyllingDTO()
            }

            call.respond(alleMeldekort)
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

            brukersMeldekortService.lagreBrukersMeldekort(meldekortFraUtfyllingDTO.toDomain())

            call.respond(HttpStatusCode.OK)
        }
    }
}
