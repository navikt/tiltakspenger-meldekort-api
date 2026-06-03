package no.nav.tiltakspenger.meldekort.landingsside.infra.routes

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
import no.nav.tiltakspenger.meldekort.landingsside.FellesLandingssideService
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideMeldekort
import no.nav.tiltakspenger.meldekort.landingsside.LandingssideStatus
import java.time.LocalDateTime

/**
 * Endepunkter som kalles fra felles landingsside for meldekortytelsene.
 * Eier auth-provider [IdentityProvider.TOKENX] og path-prefiks `/landingsside`.
 */
fun Routing.landingssideModule(applicationContext: ApplicationContext) {
    authenticate(IdentityProvider.TOKENX.value) {
        route("/landingsside") {
            fellesLandingssideRoutes(
                fellesLandingssideService = applicationContext.fellesLandingssideService,
            )
        }
    }
}

internal fun Route.fellesLandingssideRoutes(
    fellesLandingssideService: FellesLandingssideService,
) {
    // Serverer status for meldekort for en bruker
    get("/status") {
        val fnr = call.fnr()
        val landingssideStatus = fellesLandingssideService.hentLandingssideStatus(fnr)

        if (landingssideStatus == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        call.respond(HttpStatusCode.OK, landingssideStatus.tilLandingssideStatusDTO())
    }
}

private fun LandingssideStatus.tilLandingssideStatusDTO(): LandingssideStatusDTO {
    return LandingssideStatusDTO(
        harInnsendteMeldekort = harInnsendteMeldekort,
        meldekortTilUtfylling = meldekortTilUtfylling.map { it.tilLandingssideMeldekortDTO() },
        redirectUrl = redirectUrl,
    )
}

private fun LandingssideMeldekort.tilLandingssideMeldekortDTO(): LandingssideStatusDTO.LandingssideMeldekortDTO {
    return LandingssideStatusDTO.LandingssideMeldekortDTO(
        kanSendesFra = kanSendesFra,
    )
}

/**
 *  [harInnsendteMeldekort] true dersom brukeren har sendt inn meldekort tidligere
 *  [meldekortTilUtfylling] Liste over meldekort som er klare til utfylling
 *  [redirectUrl] URL som lenkes til fra felles landingsside
 */
private data class LandingssideStatusDTO(
    val harInnsendteMeldekort: Boolean,
    val meldekortTilUtfylling: List<LandingssideMeldekortDTO>,
    val redirectUrl: String,
) {
    /**
     *  [kanSendesFra] Tidspunkt der meldekortet blir tilgjengelig for innsending
     *  [kanFyllesUtFra] Vi tillater utfylling og innsending fra samme tidspunkt
     *  [fristForInnsending] Vi har ingen frist for innsending nå, men dette kommer antagelig på plass når samtlige brukere er ute av Arena
     */
    data class LandingssideMeldekortDTO(
        val kanSendesFra: LocalDateTime,
    ) {
        val kanFyllesUtFra: LocalDateTime = kanSendesFra
        val fristForInnsending: LocalDateTime? = null
    }
}
