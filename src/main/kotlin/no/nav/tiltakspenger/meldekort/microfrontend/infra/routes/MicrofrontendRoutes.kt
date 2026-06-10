package no.nav.tiltakspenger.meldekort.microfrontend.infra.routes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.microfrontend.HentMeldekortInfoForMicrofrontendService

private val log = KotlinLogging.logger { }

/**
 * Endepunkter som kalles fra brukers meldekort-microfrontend.
 * Eier auth-provider [IdentityProvider.TOKENX] og path-prefiks `/din-side/microfrontend`.
 */
fun Routing.microfrontendModule(
    hentMeldekortInfoForMicrofrontendService: HentMeldekortInfoForMicrofrontendService,
) {
    authenticate(IdentityProvider.TOKENX.value) {
        route("/din-side/microfrontend") {
            microfrontendRoutes(
                hentMeldekortInfoForMicrofrontendService = hentMeldekortInfoForMicrofrontendService,
            )
        }
    }
}

/**
 * Serialiseres til JSON via [no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendMeldekortInfo.toDTO].
 */
fun Route.microfrontendRoutes(
    hentMeldekortInfoForMicrofrontendService: HentMeldekortInfoForMicrofrontendService,
) {
    get("/meldekort-kort-info") {
        hentMeldekortInfoForMicrofrontendService.hentInformasjonOmMeldekortForMicrofrontend(call.fnr()).fold(
            ifLeft = {
                log.error(it.throwable) { "Kunne ikke hente meldekort-info til microfrontend" }
                call.respond(HttpStatusCode.InternalServerError)
            },
            ifRight = { meldekortInfo ->
                call.respondText(meldekortInfo.toDTO(), ContentType.Application.Json, HttpStatusCode.OK)
            },
        )
    }
}
