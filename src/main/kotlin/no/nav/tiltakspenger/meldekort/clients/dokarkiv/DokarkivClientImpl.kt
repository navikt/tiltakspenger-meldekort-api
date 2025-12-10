package no.nav.tiltakspenger.meldekort.clients.dokarkiv

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.meldekort.clients.httpClientWithRetry
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import org.slf4j.LoggerFactory

/***
 * Dokarkiv (tidligere Joark) er en tjeneste som brukes for å opprette journalposter i Nav.
 * https://confluence.adeo.no/display/BOA/opprettJournalpost
 * swagger: https://dokarkiv-q2.dev.intern.nav.no/swagger-ui/index.html#/
 */
const val INDIVIDSTONAD = "IND"
const val DOKARKIV_PATH = "rest/journalpostapi/v1/journalpost"

class DokarkivClientImpl(
    private val client: HttpClient = httpClientWithRetry(timeout = 30L),
    private val baseUrl: String,
    private val getToken: suspend () -> AccessToken,
) : DokarkivClient {
    private val log = LoggerFactory.getLogger(this::class.java)

    override suspend fun journalførMeldekort(
        request: JournalpostRequest,
        meldekort: Meldekort,
        callId: CorrelationId,
    ): JournalpostId {
        val meldekortId = meldekort.id

        try {
            log.info("Henter credentials for å arkivere i Dokarkiv")
            val token = getToken().token
            log.info("Hent credentials til arkiv OK. Starter journalføring av søknad")
            val res = client.post("$baseUrl/$DOKARKIV_PATH") {
                accept(ContentType.Application.Json)
                header("X-Correlation-ID", INDIVIDSTONAD)
                header("Nav-Callid", callId)
                parameter("forsoekFerdigstill", request.kanFerdigstilleAutomatisk())
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(objectMapper.writeValueAsString(request))
            }
            val httpResponse = res.call.response

            if (!httpResponse.status.isSuccess() && httpResponse.status != HttpStatusCode.Conflict) {
                throw IllegalStateException("Feil ved journalføring av meldekort $meldekortId. Status: ${httpResponse.status}")
            }

            val responseBody = res.call.body<DokarkivResponse>()
            if (httpResponse.status == HttpStatusCode.Conflict) {
                log.warn("Meldekort med id $meldekortId har allerede blitt journalført (409 Conflict) med journalpostId ${responseBody.journalpostId}")
            } else {
                log.info("Vi har opprettet journalpost med id: ${responseBody.journalpostId} for meldekort $meldekortId")
            }

            if (request.kanFerdigstilleAutomatisk() && !responseBody.journalpostferdigstilt) {
                log.error("Journalpost ${responseBody.journalpostId} for meldekort $meldekortId ble ikke ferdigstilt")
            }
            return responseBody.journalpostId
        } catch (throwable: Throwable) {
            throw RuntimeException("DokarkivClient: Fikk en ukjent exception.", throwable)
        }
    }

    data class DokarkivResponse(
        val journalpostId: JournalpostId,
        val journalpostferdigstilt: Boolean,
    )
}
