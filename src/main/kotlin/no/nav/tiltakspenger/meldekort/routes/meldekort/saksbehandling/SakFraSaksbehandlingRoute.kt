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
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.meldekort.service.FeilVedMottakAvSak
import no.nav.tiltakspenger.meldekort.service.LagreFraSaksbehandlingService

fun Route.sakFraSaksbehandlingRoute(
    lagreFraSaksbehandlingService: LagreFraSaksbehandlingService,
) {
    val logger = KotlinLogging.logger {}

    // Tar i mot saker fra saksbehandling-api
    post("/sak") {
        val sakDTO = Either.catch {
            deserialize<SakTilMeldekortApiDTO>(call.receiveText())
        }.getOrElse {
            with("Feil ved parsing av sak fra saksbehandling-api") {
                logger.error { this }
                Sikkerlogg.error(it) { this }
                call.respond(message = this, status = HttpStatusCode.BadRequest)
            }
            return@post
        }

        lagreFraSaksbehandlingService.lagre(sakDTO).onLeft {
            when (it) {
                FeilVedMottakAvSak.FinnesUtenDiff -> call.respond(
                    message = "Saken var allerede lagret med samme data",
                    status = HttpStatusCode.OK,
                )

                FeilVedMottakAvSak.MeldeperiodeFinnesMedDiff -> call.respond(
                    message = "Meldeperiode var allerede lagret med andre data",
                    status = HttpStatusCode.Conflict,
                )

                FeilVedMottakAvSak.OpprettSakFeilet -> call.respond(
                    message = "Opprettelse av sak feilet",
                    status = HttpStatusCode.InternalServerError,
                )

                FeilVedMottakAvSak.LagringFeilet -> call.respond(
                    message = "Lagring av sak feilet",
                    status = HttpStatusCode.InternalServerError,
                )
            }
        }.onRight {
            call.respond(message = "Sak lagret", status = HttpStatusCode.OK)
        }
    }
}
