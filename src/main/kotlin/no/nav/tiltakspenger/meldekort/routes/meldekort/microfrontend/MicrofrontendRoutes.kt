package no.nav.tiltakspenger.meldekort.routes.meldekort.microfrontend

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.service.MeldekortService

fun Route.microfrontendRoutes(
    meldekortService: MeldekortService,
) {
    get("microfrontend/kort-info") {
        meldekortService.hentInformasjonOmMeldekortForMicrofrontend(call.fnr()).let {
            val (antallMeldekortKlarTilInnsending, nesteMuligeInnsending) = it

            call.respond(
                HttpStatusCode.OK,
                MicrofrontendKortDTO(
                    antallMeldekortKlarTilInnsending = antallMeldekortKlarTilInnsending,
                    nesteMuligeInnsending = nesteMuligeInnsending,
                ),
            )
        }
    }
}
