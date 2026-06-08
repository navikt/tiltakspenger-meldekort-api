package no.nav.tiltakspenger.meldekort.microfrontend.infra.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.infra.ApplicationContext
import no.nav.tiltakspenger.meldekort.microfrontend.HentMeldekortInfoForMicrofrontendService

/**
 * Endepunkter som kalles fra brukers meldekort-microfrontend.
 * Eier auth-provider [IdentityProvider.TOKENX] og path-prefiks `/din-side/microfrontend`.
 */
fun Routing.microfrontendModule(applicationContext: ApplicationContext) {
    authenticate(IdentityProvider.TOKENX.value) {
        route("/din-side/microfrontend") {
            microfrontendRoutes(
                hentMeldekortInfoForMicrofrontendService = applicationContext.hentMeldekortInfoForMicrofrontendService,
            )
        }
    }
}

/**
 * Response DTO: [MicrofrontendKortDTO]
 */
internal fun Route.microfrontendRoutes(
    hentMeldekortInfoForMicrofrontendService: HentMeldekortInfoForMicrofrontendService,
) {
    get("/meldekort-kort-info") {
        hentMeldekortInfoForMicrofrontendService.hentInformasjonOmMeldekortForMicrofrontend(call.fnr()).let {
            val (antallMeldekortKlarTilInnsending, nesteMuligeInnsending) = it

            call.respond(
                HttpStatusCode.OK,
                MicrofrontendKortDTO(
                    antallMeldekortKlarTilInnsending = antallMeldekortKlarTilInnsending,
                    nesteMuligeInnsendingstidspunkt = nesteMuligeInnsending,
                ),
            )
        }
    }
}
