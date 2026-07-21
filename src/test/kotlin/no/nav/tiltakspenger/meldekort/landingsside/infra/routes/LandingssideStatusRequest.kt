package no.nav.tiltakspenger.meldekort.landingsside.infra.routes

import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.tilForventetBody
import java.time.LocalDateTime

/**
 * Route: [no.nav.tiltakspenger.meldekort.landingsside.infra.routes.fellesLandingssideRoutes]
 */
internal suspend fun ApplicationTestBuilder.landingssideStatusRequest(
    fnr: String,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): LandingssideStatusResponsDTO? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.Get,
        uri = url {
            protocol = URLProtocol.HTTPS
            path("/landingsside/status")
        },
        jwt = jwt,
        forventet = ForventetRespons(
            status = forventetStatus,
            body = forventetBody.tilForventetBody(),
            contentType = forventetContentType,
        ),
    )
    return if (response.status == HttpStatusCode.OK) {
        deserialize<LandingssideStatusResponsDTO>(response.bodyAsText())
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
