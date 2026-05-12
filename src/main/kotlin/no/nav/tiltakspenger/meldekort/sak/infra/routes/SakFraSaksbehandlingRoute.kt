package no.nav.tiltakspenger.meldekort.sak.infra.routes

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.meldekort.infra.ApplicationContext
import no.nav.tiltakspenger.meldekort.sak.FeilVedMottakAvSak
import no.nav.tiltakspenger.meldekort.sak.LagreFraSaksbehandlingService
import no.nav.tiltakspenger.meldekort.sak.infra.tilSak

/**
 * Endepunkter som kalles fra saksbehandling-api.
 * Eier auth-provider [IdentityProvider.AZUREAD] og path-prefiks `/saksbehandling`.
 */
fun Routing.saksbehandlingModule(applicationContext: ApplicationContext) {
    authenticate(IdentityProvider.AZUREAD.value) {
        route("/saksbehandling") {
            sakFraSaksbehandlingRoute(
                lagreFraSaksbehandlingService = applicationContext.lagreFraSaksbehandlingService,
            )
        }
    }
}

/**
 * Request DTO: [SakTilMeldekortApiDTO]
 */
internal fun Route.sakFraSaksbehandlingRoute(
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

        val sak = try {
            sakDTO.tilSak()
        } catch (e: IllegalArgumentException) {
            // `require(...)` i init-blokkene på Sak/Meldekortvedtak/Meldeperiode (samt
            // *.fromString-kall) kaster IllegalArgumentException når payloaden bryter
            // en domeneinvariant. Dette er klientfeil → 400, slik at avsender ikke
            // retrier eller får alarmer. Andre exceptions er uventede og lar vi boble
            // opp til ktor sin standard feilhåndtering (500).
            with("Ugyldig sak fra saksbehandling-api (brudd på domeneinvariant). sakId: ${sakDTO.sakId}") {
                logger.warn { "$this. Se meldekort-api sin sikkerlogg for detaljer." }
                Sikkerlogg.warn(e) { this }
                call.respond(message = this, status = HttpStatusCode.BadRequest)
            }
            return@post
        }

        lagreFraSaksbehandlingService.lagre(sak).onLeft {
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
