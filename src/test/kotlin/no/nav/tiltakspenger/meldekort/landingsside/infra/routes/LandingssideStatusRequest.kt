package no.nav.tiltakspenger.meldekort.landingsside.infra.routes

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import java.time.LocalDateTime

/**
 * Route: [no.nav.tiltakspenger.meldekort.landingsside.infra.routes.fellesLandingssideRoutes]
 */
internal suspend fun ApplicationTestBuilder.landingssideStatusRequest(
    fnr: String,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json"),
): LandingssideStatusResponsDTO? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.GET,
        uri = "/landingsside/status",
        jwt = jwt,
        forventet = forventet,
    )
    return if (response.statusCode == 200) {
        deserialize<LandingssideStatusResponsDTO>(response.body)
    } else {
        null
    }
}

internal data class LandingssideStatusResponsDTO(
    val harInnsendteMeldekort: Boolean,
    val meldekortTilUtfylling: List<LandingssideMeldekortDTO>,
    val redirectUrl: String,
) {
    data class LandingssideMeldekortDTO(
        val kanSendesFra: LocalDateTime,
        val kanFyllesUtFra: LocalDateTime,
        val fristForInnsending: LocalDateTime?,
    )
}
