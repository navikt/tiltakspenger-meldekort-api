package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.meldekort.infra.ApplicationContext
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.korrigeringRoutes

/**
 * Endepunkter for brukers meldekort, kalt fra brukers meldekort-frontend.
 * Bruker auth-provider [IdentityProvider.TOKENX] og path-prefiks `/brukerfrontend`.
 *
 * TODO jah: `/brukerfrontend` + TOKENX deles nå mellom denne og [no.nav.tiltakspenger.meldekort.bruker.infra.routes.brukerModule].
 *  To moduler binder samme path/auth, og begge hevder i KDoc å "eie" prefikset.
 *  Vurder å samle prefiks + auth ett sted (f.eks. en delt `brukerfrontendModule { ... }` eller en konstant for path-en) i neste iterasjon.
 */
fun Routing.meldekortModule(applicationContext: ApplicationContext) {
    val hentMeldekortService = applicationContext.hentMeldekortService
    val lagreMeldekortFraBrukerService = applicationContext.lagreMeldekortFraBrukerService
    val korrigerMeldekortService = applicationContext.korrigerMeldekortService
    val brukerService = applicationContext.brukerService
    val clock = applicationContext.clock

    authenticate(IdentityProvider.TOKENX.value) {
        route("/brukerfrontend") {
            hentInnsendteMeldekortRoute(hentMeldekortService, brukerService, clock)
            hentMeldekortForKjedeRoute(hentMeldekortService, clock)
            hentMeldekortRoute(hentMeldekortService, clock)
            sendInnMeldekortRoute(lagreMeldekortFraBrukerService)
            korrigeringRoutes(korrigerMeldekortService, clock)
        }
    }
}
