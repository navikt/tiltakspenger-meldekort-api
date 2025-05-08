package no.nav.tiltakspenger.meldekort.clients.arena

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.clients.httpClientWithRetry

class ArenaMeldekortClient(
    private val baseUrl: String,
    private val getToken: suspend () -> AccessToken,
    private val httpClient: HttpClient = httpClientWithRetry(),
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentMeldekort(fnr: Fnr): Either<ArenaMeldekortServiceFeil, ArenaMeldekortResponse?> {
        return request(fnr, "meldekortservice/api/v2/meldekort")
    }

    suspend fun hentHistoriskeMeldekort(
        fnr: Fnr,
        antallMeldeperioder: Int = 10,
    ): Either<ArenaMeldekortServiceFeil, ArenaMeldekortResponse?> {
        return request(fnr, "meldekortservice/api/v2/historiskemeldekort?antallMeldeperioder=$antallMeldeperioder")
    }

    private suspend fun request(
        fnr: Fnr,
        path: String,
    ): Either<ArenaMeldekortServiceFeil, ArenaMeldekortResponse?> {
        return Either.catch {
            val token = getToken()

            val response = httpClient.get("$baseUrl/$path") {
                accept(ContentType.Application.Json)
                bearerAuth(token.token)
                header("ident", fnr.verdi)
            }

            val status = response.status

            if (status == HttpStatusCode.OK) {
                return response.body<ArenaMeldekortResponse>().right()
            }

            if (status == HttpStatusCode.NoContent) {
                return null.right()
            }

            logger.warn { "Feil-response ved kall til meldekort-api - $status" }
            return ArenaMeldekortServiceFeil.FeilResponse(status).left()
        }.getOrElse {
            logger.warn(it) { "Ukjent feil ved request fra arena meldekortservice - ${it.message}" }
            ArenaMeldekortServiceFeil.UkjentFeil.left()
        }
    }
}

sealed interface ArenaMeldekortServiceFeil {
    data class FeilResponse(val status: HttpStatusCode) : ArenaMeldekortServiceFeil
    data object UkjentFeil : ArenaMeldekortServiceFeil
}
