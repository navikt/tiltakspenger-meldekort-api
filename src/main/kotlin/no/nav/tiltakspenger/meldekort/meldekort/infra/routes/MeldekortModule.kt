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
 *  To moduler binder samme path/auth, og begge hevder i KDoc å "eie" prefikset. Vurder å samle prefiks + auth
 *  ett sted (f.eks. en delt `brukerfrontendModule { ... }` eller en konstant for path-en) i neste iterasjon.
 */
fun Routing.meldekortModule(applicationContext: ApplicationContext) {
    val meldekortService = applicationContext.meldekortService
    val brukerService = applicationContext.brukerService
    val clock = applicationContext.clock

    authenticate(IdentityProvider.TOKENX.value) {
        route("/brukerfrontend") {
            hentInnsendteMeldekortRoute(meldekortService, brukerService, clock)
            hentMeldekortForKjedeRoute(meldekortService, clock)
            hentMeldekortRoute(meldekortService, clock)
            sendInnMeldekortRoute(meldekortService)
            korrigeringRoutes(meldekortService, clock)
        }
    }
}
