package no.nav.tiltakspenger.meldekort.routes.meldekort.saksbehandling

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.service.FeilVedMottakAvMeldeperiode
import no.nav.tiltakspenger.meldekort.service.MeldeperiodeService

fun Route.meldeperioderFraSaksbehandlingRoute(
    meldeperiodeService: MeldeperiodeService,
) {
    val logger = KotlinLogging.logger {}

    // Tar i mot meldeperioder fra saksbehandling-api
    post("/meldeperiode") {
        val meldeperiodeDto = Either.catch {
            deserialize<MeldeperiodeDTO>(call.receiveText())
        }.getOrElse {
            with("Feil ved parsing av meldeperiode fra saksbehandling-api") {
                logger.error { this }
                sikkerlogg.error(it) { this }
                call.respond(message = this, status = HttpStatusCode.BadRequest)
            }
            return@post
        }

        meldeperiodeService.lagreFraSaksbehandling(meldeperiodeDto).onLeft {
            when (it) {
                FeilVedMottakAvMeldeperiode.MeldeperiodeFinnesUtenDiff -> call.respond(
                    message = "Meldeperioden var allerede lagret med samme data",
                    status = HttpStatusCode.OK,
                )

                FeilVedMottakAvMeldeperiode.MeldeperiodeFinnesMedDiff -> call.respond(
                    message = "Meldeperioden var allerede lagret med andre data",
                    status = HttpStatusCode.Conflict,
                )

                FeilVedMottakAvMeldeperiode.UgyldigMeldeperiode -> call.respond(
                    message = "Ugyldig meldeperiode",
                    status = HttpStatusCode.BadRequest,
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
