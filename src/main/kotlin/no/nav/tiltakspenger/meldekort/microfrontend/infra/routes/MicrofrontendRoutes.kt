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
 *
 * ---
 * ## Hvordan microfrontend skrus av og på
 *
 * Microfrontenden er meldekort-boksen på «Min side» (nav.no).
 * På/av-toggelen styres ikke av denne routen, men av to periodiske jobber ([no.nav.tiltakspenger.meldekort.microfrontend.AktiverMicrofrontendJob] / [no.nav.tiltakspenger.meldekort.microfrontend.InaktiverMicrofrontendJob]) som melder av/på mot Team Min Side (TMS) og setter sak-status `UBEHANDLET`/`AKTIV`/`INAKTIV` ([no.nav.tiltakspenger.meldekort.microfrontend.infra.repo.MicrofrontendStatusDb]) så vi ikke melder det samme om igjen.
 *
 * En sak skal vises (aktiveres) hvis den har minst én meldeperiodekjede som «mangler innsending», ellers skjules den (inaktiveres).
 * Kriteriet er bevisst likt varsler-pakken (se [no.nav.tiltakspenger.meldekort.microfrontend.infra.repo.MicrofrontendPostgresRepo] `HAR_KJEDE_SOM_MANGLER_INNSENDING`), slik at kort og varsler henger sammen.
 *
 * ### Nyanser
 *  - **Vises «evig».** Kortet skrus ikke av basert på tid/alder, kun når retten faller bort eller noen har sendt inn – likt varslene.
 *  - **Framtidige perioder holder kortet åpent.** En kjede som gir rett men ennå ikke kan fylles ut (`kan_fylles_ut_fra_og_med` fram i tid) teller som åpen oppgave; kortet viser da «neste kan sendes inn \<dato\>» (antall klare = 0).
 *  - **Kun siste versjon teller (annullering/stans/opphør).** Et nytt rammevedtak lager en ny versjon av kjeden og «annullerer» den forrige; ved å se på siste versjon fanges bortfall av rett automatisk.
 *  - **Idempotens.** Feiler meldingen mot TMS står statusen urørt og prøves igjen ved neste kjøring.
 *
 * ### Routen (`GET /meldekort-kort-info`)
 * Serverer kun *innholdet* i boksen (antall klare til innsending nå + neste mulige innsendingstidspunkt), ikke om den vises, se [no.nav.tiltakspenger.meldekort.microfrontend.infra.repo.MicrofrontendPostgresRepo.hentMeldekortInfo].
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
