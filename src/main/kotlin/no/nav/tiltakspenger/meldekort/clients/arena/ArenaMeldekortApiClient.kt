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
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.meldekort.auth.TexasIdentityProvider
import no.nav.tiltakspenger.meldekort.clients.arena.response.ArenaMeldekort
import no.nav.tiltakspenger.meldekort.clients.arena.response.ArenaMeldekortStatusResponse
import no.nav.tiltakspenger.meldekort.clients.arena.response.ArenaNesteMeldekortResponse
import no.nav.tiltakspenger.meldekort.clients.httpClientWithRetry
import no.nav.tiltakspenger.meldekort.clients.texas.TexasClient

class ArenaMeldekortApiClient(
    private val baseUrl: String,
    private val audience: String,
    private val texasClient: TexasClient,
    private val httpClient: HttpClient = httpClientWithRetry(),
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentMeldekortstatus(brukerToken: String): Either<ArenaMeldekortApiFeil, ArenaMeldekortStatusResponse> {
        return request(brukerToken, "/person/meldekortstatus")
    }

    suspend fun hentNesteMeldekort(brukerToken: String): Either<ArenaMeldekortApiFeil, ArenaNesteMeldekortResponse> {
        return request(brukerToken, "/person/meldekort")
    }

    suspend fun hentHistoriskeMeldekort(brukerToken: String): Either<ArenaMeldekortApiFeil, List<ArenaMeldekort>> {
        return request(brukerToken, "/person/historiskemeldekort")
    }

    private suspend inline fun <reified ResponseType> request(
        brukerToken: String,
        path: String,
    ): Either<ArenaMeldekortApiFeil, ResponseType> {
        return Either.catch {
            val token = texasClient.exchangeToken(
                brukerToken,
                audience,
                TexasIdentityProvider.TOKENX,
            ).token

            val response = httpClient.get("$baseUrl/$path") {
                accept(ContentType.Application.Json)
                bearerAuth(token)
            }

            val status = response.status

            if (status != HttpStatusCode.OK) {
                logger.error { "Feil-response ved kall til meldekort-api - $status" }
                return ArenaMeldekortApiFeil.FeilResponse(status).left()
            }

            return response.body<ResponseType>().right()
        }.getOrElse {
            logger.error(it) { "Ukjent feil ved request fra arena meldekort-api - ${it.message}" }
            ArenaMeldekortApiFeil.UkjentFeil.left()
        }
    }
}

sealed interface ArenaMeldekortApiFeil {
    data class FeilResponse(val status: HttpStatusCode) : ArenaMeldekortApiFeil
    data object UkjentFeil : ArenaMeldekortApiFeil
}
