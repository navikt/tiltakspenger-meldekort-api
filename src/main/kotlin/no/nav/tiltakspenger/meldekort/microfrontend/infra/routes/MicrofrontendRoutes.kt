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
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortService
import java.time.Clock

/**
 * Endepunkter som kalles fra brukers meldekort-microfrontend.
 * Eier auth-provider [IdentityProvider.TOKENX] og path-prefiks `/din-side/microfrontend`.
 */
fun Routing.microfrontendModule(applicationContext: ApplicationContext) {
    authenticate(IdentityProvider.TOKENX.value) {
        route("/din-side/microfrontend") {
            microfrontendRoutes(
                meldekortService = applicationContext.meldekortService,
                clock = applicationContext.clock,
            )
        }
    }
}

/**
 * Response DTO: [MicrofrontendKortDTO]
 */
internal fun Route.microfrontendRoutes(
    meldekortService: MeldekortService,
    clock: Clock,
) {
    get("/meldekort-kort-info") {
        meldekortService.hentInformasjonOmMeldekortForMicrofrontend(call.fnr(), clock).let {
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
