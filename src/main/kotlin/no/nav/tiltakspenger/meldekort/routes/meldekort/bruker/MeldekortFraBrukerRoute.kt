package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker

import arrow.core.Either
import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.domene.MeldekortFraBrukerDTO
import no.nav.tiltakspenger.meldekort.routes.meldekort.logger
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import java.time.Clock

fun Route.meldekortFraBrukerRoute(
    meldekortService: MeldekortService,
    clock: Clock,
) {
    post("send-inn") {
        val lagreFraBrukerKommando = Either.catch {
            deserialize<MeldekortFraBrukerDTO>(call.receiveText())
                .tilLagreKommando(call.fnr(), clock)
        }.getOrElse {
            with("Feil ved parsing av innsendt meldekort fra bruker") {
                logger.error { this }
                Sikkerlogg.error(it) { "$this - ${it.message}" }
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
                Sikkerlogg.error(it) { "$this - ${it.message}" }
            }
            call.respond(HttpStatusCode.InternalServerError)
        }.onRight {
            call.respond(HttpStatusCode.OK)
        }
    }
}
