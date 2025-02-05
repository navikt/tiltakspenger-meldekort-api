package no.nav.tiltakspenger.meldekort.clients.dokarkiv

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.meldekort.clients.httpClientWithRetry
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import org.slf4j.LoggerFactory

/***
 * Dokakriv har tidligere referert til som Joark
 * https://confluence.adeo.no/display/BOA/opprettJournalpost
 * swagger: https://dokarkiv-q2.dev.intern.nav.no/swagger-ui/index.html#/
 */
const val INDIVIDSTONAD = "IND"
internal const val DOKARKIV_PATH = "rest/journalpostapi/v1/journalpost"

class DokarkivClient(
    private val client: HttpClient = httpClientWithRetry(timeout = 30L),
    private val baseUrl: String,
    private val getToken: suspend () -> AccessToken,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    suspend fun journalførMeldekort(
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
            val response = res.call.body<DokarkivResponse>()
            log.info("Vi har opprettet journalpost med id: ${response.journalpostId} for meldekort $meldekortId")

            if (request.kanFerdigstilleAutomatisk() && !response.journalpostferdigstilt) {
                log.error("Journalpost ${response.journalpostId} for meldekort $meldekortId ble opprettet, men ikke ferdigstilt")
                throw IllegalStateException("Journalpost kunne ikke ferdigstilles automatisk")
            }
            return response.journalpostId
        } catch (throwable: Throwable) {
            if (throwable is ClientRequestException && throwable.response.status == HttpStatusCode.Conflict) {
                val response = throwable.response.call.body<DokarkivResponse>()
                log.info("Meldekort med id $meldekortId har allerede blitt journalført (409 Conflict) med journalpostId ${response.journalpostId}")
                return response.journalpostId
            }
            if (throwable is IllegalStateException) {
                log.error("Vi fikk en IllegalStateException i DokarkivClient", throwable)
                throw throwable
            } else {
                log.error("DokarkivClient: Fikk en ukjent exception.", throwable)
                throw RuntimeException("DokarkivClient: Fikk en ukjent exception.", throwable)
            }
        }
    }

    data class DokarkivResponse(
        val journalpostId: JournalpostId,
        val journalpostferdigstilt: Boolean,
    )
}
