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
import no.nav.tiltakspenger.meldekort.service.FeilVedMottakAvSak
import no.nav.tiltakspenger.meldekort.service.SakDTO
import no.nav.tiltakspenger.meldekort.service.SakService

fun Route.sakFraSaksbehandlingRoute(
    sakService: SakService,
) {
    val logger = KotlinLogging.logger {}

    // Tar i mot saker fra saksbehandling-api
    post("/sak") {
        val sakDTO = Either.catch {
            deserialize<SakDTO>(call.receiveText())
        }.getOrElse {
            with("Feil ved parsing av sak fra saksbehandling-api") {
                logger.error { this }
                sikkerlogg.error(it) { this }
                call.respond(message = this, status = HttpStatusCode.BadRequest)
            }
            return@post
        }

        sakService.lagreFraSaksbehandling(sakDTO).onLeft {
            when (it) {
                FeilVedMottakAvSak.FinnesUtenDiff -> call.respond(
                    message = "Saken var allerede lagret med samme data",
                    status = HttpStatusCode.OK,
                )

                FeilVedMottakAvSak.LagringFeilet -> call.respond(
                    message = "Lagring av sak feilet",
                    status = HttpStatusCode.InternalServerError,
                )

                FeilVedMottakAvSak.OppdateringFeilet -> call.respond(
                    message = "Oppdatering av eksisterende sak feilet",
                    status = HttpStatusCode.InternalServerError,
                )
            }
        }.onRight {
            call.respond(message = "Sak lagret", status = HttpStatusCode.OK)
        }
    }
}
