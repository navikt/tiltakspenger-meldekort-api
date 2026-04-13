package no.nav.tiltakspenger.routes.hentmeldekortforkjede

import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.MeldekortForKjedeDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR
import no.nav.tiltakspenger.routes.JwtGenerator
import no.nav.tiltakspenger.routes.defaultRequestWithAssertions

/**
 * Route: [no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.hentMeldekortForKjedeRoute]
 * Response DTO: [no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.MeldekortForKjedeDTO]
 */
suspend fun ApplicationTestBuilder.hentMeldekortForKjedeIdRequest(
    kjedeId: String,
    fnr: String = FAKE_FNR,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = null,
    forventetContentType: ContentType? = ContentType.Application.Json,
): MeldekortForKjedeDTO? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.Get,
        uri = "/brukerfrontend/kjede/$kjedeId",
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    )
    return if (response.status == HttpStatusCode.OK) {
        deserialize<MeldekortForKjedeDTO>(response.bodyAsText())
    } else {
        null
    }
}
