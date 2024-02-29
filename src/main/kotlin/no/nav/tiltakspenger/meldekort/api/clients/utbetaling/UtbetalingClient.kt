package no.nav.tiltakspenger.meldekort.api.clients.utbetaling

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
import no.nav.tiltakspenger.meldekort.api.domene.MeldekortBeregning
import java.time.LocalDate
import java.util.*

val securelog = KotlinLogging.logger("tjenestekall")

class UtbetalingClient(
    private val config: Configuration.ClientConfig = Configuration.utbetalingClientConfig(),
    private val objectMapper: ObjectMapper = defaultObjectMapper(),
    private val getToken: suspend () -> String,
    engine: HttpClientEngine? = null,
    private val httpClient: HttpClient = defaultHttpClient(
        objectMapper = objectMapper,
        engine = engine,
    ),
) : Utbetaling {
    companion object {
        const val navCallIdHeader = "Nav-Call-Id"
    }

    override suspend fun sendTilUtbetaling(
        sakId: String,
        behandling: MeldekortBeregning,
    ): String {
        val httpResponse =
            httpClient.post("${config.baseUrl}/utbetaling/utbetalingvedtak") {
                header(navCallIdHeader, "tiltakspenger-meldekort-api")
                bearerAuth(getToken())
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(
                    mapUtbetalingMeldekort(sakId, behandling),
                )
            }

        return when (httpResponse.status) {
            HttpStatusCode.Accepted -> httpResponse.call.response.body()
            else -> throw RuntimeException("error (responseCode=${httpResponse.status.value}) fra Utbetaling")
        }
    }

    override suspend fun hentPeriodisertUtbetalingsgrunnlag(
        behandlingId: String,
        fom: LocalDate,
        tom: LocalDate,
    ): List<UtbetalingGrunnlagPeriode> {
        val httpResponse =
            httpClient.post("${config.baseUrl}/utbetaling/hentGrunnlag") {
                header(navCallIdHeader, "tiltakspenger-meldekort-api")
                bearerAuth(getToken())
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(
                    mapGrunnlag(behandlingId, fom, tom),
                )
            }

        return when (httpResponse.status) {
            HttpStatusCode.OK -> httpResponse.call.response.body()
            else -> throw RuntimeException("error (responseCode=${httpResponse.status.value}) fra Utbetaling")
        }
    }
}
