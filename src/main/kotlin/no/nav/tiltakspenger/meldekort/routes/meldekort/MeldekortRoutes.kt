package no.nav.tiltakspenger.meldekort.routes.meldekort

import arrow.core.getOrElse
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
import no.nav.tiltakspenger.meldekort.auth.fnr
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
                call.respond(message = "Deserialize av meldeperiode feilet", status = HttpStatusCode.BadRequest)
                return@handle
            }

            val meldekort = meldeperiode.tilMeldeperiode().getOrElse {
                call.respond(message = "Ugyldig meldeperiode: ${it.message}", status = HttpStatusCode.BadRequest)
                return@handle
            }

            if (meldeperiodeService.hentMeldeperiodeForId(meldekort.id) != null) {
                call.respond(message = "Meldeperioden finnes allerede", status = HttpStatusCode.Conflict)
                return@handle
            }

            // Disse to må være en transaksjon
            meldeperiodeService.lagreMeldeperiode(meldekort).onLeft {
                call.respond(message = "Lagring av meldeperiode feilet", status = HttpStatusCode.InternalServerError)
                return@handle
            }
            brukersMeldekortService.opprettFraMeldeperiode(meldekort)

            call.respond(HttpStatusCode.OK)
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
            val meldekort = brukersMeldekortService.hentSisteMeldekort(call.fnr())
            if (meldekort == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respond(meldekort.tilUtfyllingDTO())
        }

        get("alle") {
            val alleMeldekort = brukersMeldekortService.hentAlleMeldekort(call.fnr()).map {
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

            brukersMeldekortService.lagreBrukersMeldekort(
                meldekort = meldekortFraUtfyllingDTO.toDomain(),
                fnr = call.fnr(),
            )

            call.respond(HttpStatusCode.OK)
        }
    }
}
