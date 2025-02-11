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
import no.nav.tiltakspenger.meldekort.domene.MeldekortFraBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.tilBrukerDTO
import no.nav.tiltakspenger.meldekort.service.FeilVedMottakAvMeldeperiode
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import no.nav.tiltakspenger.meldekort.service.MeldeperiodeService

val logger = KotlinLogging.logger {}

internal fun Route.meldekortRoutes(
    meldekortService: MeldekortService,
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

            meldekortService.hentForMeldekortId(meldekortId, call.fnr())?.also {
                call.respond(it.tilBrukerDTO())
                return@get
            }

            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        get("siste") {
            val meldekort = meldekortService.hentSisteMeldekort(call.fnr())
            if (meldekort == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respond(meldekort.tilBrukerDTO())
        }

        get("alle") {
            val alleMeldekort = meldekortService.hentAlleMeldekort(call.fnr()).map {
                it.tilBrukerDTO()
            }

            call.respond(alleMeldekort)
        }

        post("send-inn") {
            val lagreFraBrukerKommando = Either.catch {
                deserialize<MeldekortFraBrukerDTO>(call.receiveText())
                    .tilLagreKommando(call.fnr())
            }.getOrElse {
                with("Feil ved parsing av innsendt meldekort fra bruker") {
                    logger.error { this }
                    sikkerlogg.error(it) { "$this - ${it.message}" }
                }
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            Either.catch {
                meldekortService.lagreMeldekortFraBruker(
                    kommando = lagreFraBrukerKommando,
                )
            }.onLeft {
                with("Feil ved lagring av innsendt meldekort fra bruker") {
                    logger.error { "Feil ved lagring av innsendt meldekort med id ${lagreFraBrukerKommando.id}" }
                    sikkerlogg.error(it) { "$this - ${it.message}" }
                }
                call.respond(HttpStatusCode.InternalServerError)
            }.onRight {
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
