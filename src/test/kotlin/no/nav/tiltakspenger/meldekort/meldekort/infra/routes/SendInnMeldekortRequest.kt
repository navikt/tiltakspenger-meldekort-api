package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

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
import no.nav.tiltakspenger.meldekort.bruker.infra.routes.hentBrukerMedSakRequest
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.defaultRequestWithAssertions
import no.nav.tiltakspenger.meldekort.infra.routes.jobber.KjørJobberForTester
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortDagFraBrukerDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortDagStatusDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortFraBrukerDTO
import no.nav.tiltakspenger.meldekort.sak.Sak
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR

/**
 * Henter brukerens neste meldekort via [hentBrukerMedSakRequest], bygger request-body, sender inn via [sendInnMeldekortRequest],
 * og returnerer nyttig info for videre kjeding.
 */
suspend fun ApplicationTestBuilder.sendInnNesteMeldekort(
    tac: TestApplicationContext,
    fnr: String = FAKE_FNR,
    locale: String? = null,
    runJobs: Boolean = true,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = "",
    forventetContentType: ContentType? = null,
): Pair<Sak, BrukersMeldekort>? {
    val bruker = hentBrukerMedSakRequest(fnr, jwt)!!
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
        requestDto = requestBody,
        fnr = Fnr.fromString(fnr),
        runJobs = runJobs,
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    )
}

/**
 * Henter brukerens neste meldekort via [hentBrukerMedSakRequest], bygger request-body, sender inn via [sendInnMeldekortRequest],
 * og returnerer nyttig info for videre kjeding.
 */
@Suppress("unused")
suspend fun ApplicationTestBuilder.sendInnMeldekortViaBruker(
    tac: TestApplicationContext,
    meldekortId: MeldekortId,
    dager: List<MeldekortDagFraBrukerDTO>,
    locale: String? = null,
    fnr: String = FAKE_FNR,
    runJobs: Boolean = true,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = "",
    forventetContentType: ContentType? = null,
): Pair<Sak, BrukersMeldekort>? {
    val bruker = hentBrukerMedSakRequest(fnr, jwt)!!
    val nesteMeldekort = bruker.nesteMeldekort
    nesteMeldekort shouldNotBe null

    val requestBody = MeldekortFraBrukerDTO(
        id = meldekortId.toString(),
        dager = dager,
        locale = locale,
    )

    return sendInnMeldekortRequest(
        tac = tac,
        requestDto = requestBody,
        fnr = Fnr.fromString(fnr),
        runJobs = runJobs,
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    )
}

/**
 * Route: [no.nav.tiltakspenger.meldekort.meldekort.infra.routes.sendInnMeldekortRoute]
 * Request DTO: [MeldekortFraBrukerDTO]
 */
suspend fun ApplicationTestBuilder.sendInnMeldekortRequest(
    tac: TestApplicationContext,
    requestDto: MeldekortFraBrukerDTO,
    fnr: Fnr = Fnr.fromString(FAKE_FNR),
    runJobs: Boolean = true,
    jwt: String? = JwtGenerator().createJwtForUser(fnr = fnr.toString()),
    forventetStatus: HttpStatusCode = HttpStatusCode.OK,
    forventetBody: String? = "",
    forventetContentType: ContentType? = null,
): Pair<Sak, BrukersMeldekort>? {
    val response = defaultRequestWithAssertions(
        method = HttpMethod.Post,
        uri = "/brukerfrontend/send-inn",
        jwt = jwt,
        forventetStatus = forventetStatus,
        forventetBody = forventetBody,
        forventetContentType = forventetContentType,
    ) {
        setBody(serialize(requestDto))
    }
    if (response.status != HttpStatusCode.OK) return null
    if (runJobs) {
        KjørJobberForTester.kjørVarsler(tac)
    }
    val innsendtMeldekort = tac.meldekortService.hentForMeldekortId(MeldekortId.fromString(requestDto.id), fnr)!!
    // Hent den fulle saken via meldekortets sakId — gir den joined varianten, som testene normalt vil ha.
    val sak = tac.sakRepo.hent(innsendtMeldekort.sakId, null)!!
    return sak to innsendtMeldekort
}
