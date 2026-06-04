package no.nav.tiltakspenger.meldekort.mottak.infra.routes

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
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.meldekort.mottak.FeilVedMottakAvSak
import no.nav.tiltakspenger.meldekort.mottak.MottakFraSaksbehandlingService
import no.nav.tiltakspenger.meldekort.mottak.infra.tilMottattSak

/**
 * Endepunkter som kalles fra saksbehandling-api.
 * Eier auth-provider [IdentityProvider.AZUREAD] og path-prefiks `/saksbehandling`.
 *
 * Request DTO: [SakTilMeldekortApiDTO]
 */
internal fun Route.mottakFraSaksbehandlingRoute(
    mottakFraSaksbehandlingService: MottakFraSaksbehandlingService,
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

        val mottattSak = Either.catch {
            sakDTO.tilMottattSak()
        }.getOrElse {
            with("Feil ved mapping av sak-DTO til domenemodell under mottak av sak fra saksbehandling-api. sakId: ${sakDTO.sakId}. Antall meldeperioder: ${sakDTO.meldeperioder.size}. Antall meldekortvedtak: ${sakDTO.meldekortvedtak.size}.") {
                logger.warn { "$this. Se meldekort-api sin sikkerlogg (GCP) for detaljer." }
                Sikkerlogg.warn(it) { this }
                call.respond(message = this, status = HttpStatusCode.BadRequest)
            }
            return@post
        }

        mottakFraSaksbehandlingService.lagre(mottattSak).onLeft {
            when (it) {
                FeilVedMottakAvSak.FinnesUtenDiff -> call.respond(
                    message = "Saken var allerede lagret med samme data",
                    status = HttpStatusCode.OK,
                )

                FeilVedMottakAvSak.MeldeperiodeFinnesMedDiff -> call.respond(
                    message = "Meldeperiode var allerede lagret med andre data",
                    status = HttpStatusCode.Conflict,
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
