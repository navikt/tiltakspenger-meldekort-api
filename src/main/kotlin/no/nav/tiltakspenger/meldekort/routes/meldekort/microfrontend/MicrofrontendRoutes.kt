package no.nav.tiltakspenger.meldekort.routes.meldekort.microfrontend

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tiltakspenger.libs.texas.fnr
import no.nav.tiltakspenger.meldekort.service.MeldekortService
import java.time.Clock

fun Route.microfrontendRoutes(
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
                    nesteMuligeInnsending = nesteMuligeInnsending?.toLocalDate(),
                    nesteMuligeInnsendingstidspunkt = nesteMuligeInnsending,
                ),
            )
        }
    }
}
