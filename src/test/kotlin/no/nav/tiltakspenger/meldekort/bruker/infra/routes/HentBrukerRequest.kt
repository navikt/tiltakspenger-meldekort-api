package no.nav.tiltakspenger.meldekort.bruker.infra.routes

import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.defaultRequestWithAssertions
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR

/**
 * Route: [no.nav.tiltakspenger.meldekort.bruker.infra.routes.hentBrukerRoute]
 * Response DTO: [BrukerDTO.MedSak] eller null
 * @param fnr brukes kun for default verdi av [jwt]
 * @return null dersom kallet feilet (ikke status 200 OK)
 */
suspend fun ApplicationTestBuilder.hentBrukerMedSakRequest(
    fnr: String = FAKE_FNR,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): BrukerDTO.MedSak? = hentBrukerResponseAs(
    fnr = fnr,
    jwt = jwt,
    forventetStatus = forventetStatus,
    forventetBody = forventetBody,
    forventetContentType = forventetContentType,
)

/**
 * Route: [no.nav.tiltakspenger.meldekort.bruker.infra.routes.hentBrukerRoute]
 * Response DTO: [BrukerDTO.UtenSak] eller null
 * @param fnr brukes kun for default verdi av [jwt]
 * @return null dersom kallet feilet (ikke status 200 OK)
 */
suspend fun ApplicationTestBuilder.hentBrukerUtenSakRequest(
    fnr: String = FAKE_FNR,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): BrukerDTO.UtenSak? = hentBrukerResponseAs(
    fnr = fnr,
    jwt = jwt,
    forventetStatus = forventetStatus,
    forventetBody = forventetBody,
    forventetContentType = forventetContentType,
)

/**
 * Deserialiserer rett til konkret subtype [T]. Vi har ikke Jackson-diskriminator på [BrukerDTO]
 * i prod, så tester må vite hvilken variant de forventer (slik en typet frontend også gjør).
 */
private suspend inline fun <reified T : BrukerDTO> ApplicationTestBuilder.hentBrukerResponseAs(
    fnr: String,
    jwt: String?,
    forventetStatus: HttpStatusCode,
    forventetBody: String?,
    forventetContentType: ContentType?,
): T? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.Get,
        uri = url {
            protocol = URLProtocol.HTTPS
            path("/brukerfrontend/bruker")
        },
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    )
    return if (response.status == HttpStatusCode.OK) {
        deserialize<T>(response.bodyAsText())
    } else {
        null
    }
}
