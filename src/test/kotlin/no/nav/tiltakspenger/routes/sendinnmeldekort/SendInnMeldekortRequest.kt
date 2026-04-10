package no.nav.tiltakspenger.routes.sendinnmeldekort

import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagFraBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatusDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortFraBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.Sak
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR
import no.nav.tiltakspenger.routes.JwtGenerator
import no.nav.tiltakspenger.routes.defaultRequestWithAssertions
import no.nav.tiltakspenger.routes.hentbruker.hentBrukerRequest

/**
 * Henter brukerens neste meldekort via [hentBrukerRequest], bygger request-body, sender inn via [sendInnMeldekortRequest],
 * og returnerer nyttig info for videre kjeding.
 */
suspend fun ApplicationTestBuilder.sendInnNesteMeldekort(
    tac: TestApplicationContext,
    fnr: String = FAKE_FNR,
    locale: String? = null,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = "",
    forventetContentType: ContentType? = null,
): Pair<Sak, Meldekort>? {
    val bruker = hentBrukerRequest(fnr, jwt)!!
    val nesteMeldekort = bruker.nesteMeldekort!!

    val requestBody = MeldekortFraBrukerDTO(
        id = nesteMeldekort.id,
        dager = nesteMeldekort.dager.map {
            MeldekortDagFraBrukerDTO(
                dag = it.dag,
                status = if (it.status == MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER) {
                    MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
                } else {
                    MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET
                },
            )
        },
        locale = locale,
    )
    return sendInnMeldekortRequest(
        tac = tac,
        requestBody = requestBody,
        fnr = Fnr.fromString(fnr),
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    )
}

/**
 * Henter brukerens neste meldekort via [hentBrukerRequest], bygger request-body, sender inn via [sendInnMeldekortRequest],
 * og returnerer nyttig info for videre kjeding.
 */
@Suppress("unused")
suspend fun ApplicationTestBuilder.sendInnMeldekortViaBruker(
    tac: TestApplicationContext,
    meldekortId: MeldekortId,
    dager: List<MeldekortDagFraBrukerDTO>,
    locale: String? = null,
    fnr: String = FAKE_FNR,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = "",
    forventetContentType: ContentType? = null,
): Pair<Sak, Meldekort>? {
    val bruker = hentBrukerRequest(fnr, jwt)!!
    val nesteMeldekort = bruker.nesteMeldekort
    nesteMeldekort shouldNotBe null

    val requestBody = MeldekortFraBrukerDTO(
        id = meldekortId.toString(),
        dager = dager,
        locale = locale,
    )

    return sendInnMeldekortRequest(
        tac = tac,
        requestBody = requestBody,
        fnr = Fnr.fromString(fnr),
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    )
}

/**
 * Route: [no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.sendInnMeldekortRoute]
 * Request DTO: [no.nav.tiltakspenger.meldekort.domene.MeldekortFraBrukerDTO]
 */
suspend fun ApplicationTestBuilder.sendInnMeldekortRequest(
    tac: TestApplicationContext,
    requestBody: MeldekortFraBrukerDTO,
    fnr: Fnr = Fnr.fromString(FAKE_FNR),
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr.toString()),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = "",
    forventetContentType: ContentType? = null,
): Pair<Sak, Meldekort>? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.Post,
        uri = "/brukerfrontend/send-inn",
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    ) {
        setBody(serialize(requestBody))
    }
    if (response.status != HttpStatusCode.OK) return null
    val sak = tac.sakRepo.hentTilBruker(fnr, null)!!
    val innsendtMeldekort = tac.meldekortService.hentForMeldekortId(MeldekortId.fromString(requestBody.id), fnr)!!
    return sak to innsendtMeldekort
}
