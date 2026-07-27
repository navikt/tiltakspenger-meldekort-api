package no.nav.tiltakspenger.meldekort.bruker.infra.routes

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.tilForventetBody

/**
 * Route: [no.nav.tiltakspenger.meldekort.bruker.infra.routes.hentBrukerRoute]
 * Response DTO: [BrukerDTO.MedSak] eller null
 * @param fnr identiteten kallet autentiseres som (legges i `pid`-claimet på [jwt]).
 * @return null dersom kallet feilet (ikke status 200 OK)
 */
suspend fun ApplicationTestBuilder.hentBrukerMedSakRequest(
    fnr: String,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json"),
): BrukerDTO.MedSak? = hentBrukerResponseAs(
    fnr = fnr,
    jwt = jwt,
    forventet = forventet,
)

/**
 * Route: [no.nav.tiltakspenger.meldekort.bruker.infra.routes.hentBrukerRoute]
 * Response DTO: [BrukerDTO.UtenSak] eller null
 * @param fnr identiteten kallet autentiseres som (legges i `pid`-claimet på [jwt]).
 * @return null dersom kallet feilet (ikke status 200 OK)
 */
suspend fun ApplicationTestBuilder.hentBrukerUtenSakRequest(
    fnr: String,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json"),
): BrukerDTO.UtenSak? = hentBrukerResponseAs(
    fnr = fnr,
    jwt = jwt,
    forventet = forventet,
)

/**
 * Deserialiserer rett til konkret subtype [T].
 * Vi har ikke Jackson-diskriminator på [BrukerDTO] i prod, så tester må vite hvilken variant de forventer (slik en typet frontend også gjør).
 */
private suspend inline fun <reified T : BrukerDTO> ApplicationTestBuilder.hentBrukerResponseAs(
    fnr: String,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventet: ForventetRespons?,
): T? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.GET,
        uri = "/brukerfrontend/bruker",
        jwt = jwt,
        forventet = forventet,
    )
    return if (response.statusCode == 200) {
        deserialize<T>(response.body)
    } else {
        null
    }
}
