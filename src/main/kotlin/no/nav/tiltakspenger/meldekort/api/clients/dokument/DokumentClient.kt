package no.nav.tiltakspenger.meldekort.api.clients.dokument

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.api.Configuration
import no.nav.tiltakspenger.meldekort.api.clients.defaultHttpClient
import no.nav.tiltakspenger.meldekort.api.clients.defaultObjectMapper
import no.nav.tiltakspenger.meldekort.api.domene.Meldekort
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortGrunnlag
import java.time.LocalDateTime

private val LOG = KotlinLogging.logger {}
class DokumentClient(
    private val config: Configuration.ClientConfig = Configuration.dokumentClientConfig(),
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
    private val getToken: suspend () -> String,
    engine: HttpClientEngine? = null,
    private val httpClient: HttpClient = defaultHttpClient(
        objectMapper = objectMapper,
        engine = engine,
    ),
) : Dokument {
    companion object {
        const val navCallIdHeader = "Nav-Call-Id"
    }

    override suspend fun sendMeldekortTilDokument(meldekort: Meldekort?, grunnlag: MeldekortGrunnlag): JoarkResponse {
        LOG.info { "Request motatt for å sende meldekort til tiltakspenger-dokument på /arkivMeldekort" }

        val httpResponse = httpClient.post("${config.baseUrl}/arkivMeldekort") {
            header(navCallIdHeader, "tiltakspenger-meldekort-api")
            bearerAuth(getToken())
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(
                mapMeldekortDTOTilDokumentDTO(meldekort, grunnlag),
            )
        }

        return when (httpResponse.status) {
            HttpStatusCode.OK -> httpResponse.call.response.body()
            else -> throw RuntimeException("error (responseCode=${httpResponse.status.value}) fra Dokument")
        }
    }
}

data class JoarkResponse(
    val journalpostId: String,
    val innsendingTidspunkt: LocalDateTime,
)
