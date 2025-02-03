package no.nav.tiltakspenger.meldekort.routes.meldekort

import arrow.core.Either
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
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.auth.TexasWallBrukerToken
import no.nav.tiltakspenger.meldekort.auth.TexasWallSystemToken
import no.nav.tiltakspenger.meldekort.auth.fnr
import no.nav.tiltakspenger.meldekort.clients.TexasHttpClient
import no.nav.tiltakspenger.meldekort.service.BrukersMeldekortService
import no.nav.tiltakspenger.meldekort.service.FeilVedMottakAvMeldeperiode
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
            val meldeperiodeDto = Either.catch {
                deserialize<MeldeperiodeDTO>(call.receiveText())
            }.getOrElse {
                with("Feil ved parsing av meldeperiode fra saksbehandling-api") {
                    logger.error { this }
                    sikkerlogg.error(it) { this }
                    call.respond(message = this, status = HttpStatusCode.BadRequest)
                }
                return@handle
            }

            meldeperiodeService.lagreFraSaksbehandling(meldeperiodeDto).onLeft {
                when (it) {
                    FeilVedMottakAvMeldeperiode.UgyldigMeldeperiode -> call.respond(
                        message = "Ugyldig meldeperiode",
                        status = HttpStatusCode.BadRequest,
                    )

                    FeilVedMottakAvMeldeperiode.MeldeperiodeFinnes -> call.respond(
                        message = "Meldeperioden finnes allerede",
                        status = HttpStatusCode.Conflict,
                    )

                    FeilVedMottakAvMeldeperiode.LagringFeilet -> call.respond(
                        message = "Lagring av meldeperiode feilet",
                        status = HttpStatusCode.InternalServerError,
                    )
                }
            }.onRight {
                call.respond(message = "Meldeperiode lagret", status = HttpStatusCode.OK)
            }
        }
    }

    // Endepunkter som kalles fra brukers meldekort-frontend
    route("/meldekort/bruker") {
        install(TexasWallBrukerToken) {
            client = texasHttpClient
        }

        get("meldekort/{meldekortId}") {
            val meldekortId = call.parameters["meldekortId"]?.let { MeldekortId.fromString(it) }
            if (meldekortId == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            brukersMeldekortService.hentForMeldekortId(meldekortId)?.also {
                call.respond(it.tilUtfyllingDTO())
                return@get
            }

            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        get("kjede/{meldeperiodeKjedeId}") {
            val meldeperiodeKjedeIdParam = call.parameters["meldeperiodeKjedeId"]
            if (meldeperiodeKjedeIdParam == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            brukersMeldekortService.hentMeldekortForMeldeperiodeKjedeId(meldeperiodeKjedeIdParam)?.also {
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
