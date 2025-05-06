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
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.auth.TexasIdentityProvider
import no.nav.tiltakspenger.meldekort.clients.httpClientWithRetry
import no.nav.tiltakspenger.meldekort.clients.texas.TexasClient

class ArenaMeldekortServiceClient(
    private val baseUrl: String,
    private val audience: String,
    private val texasClient: TexasClient,
    private val httpClient: HttpClient = httpClientWithRetry(),
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentNesteMeldekort(fnr: Fnr): Either<ArenaMeldekortServiceFeil, ArenaMeldekortResponse> {
        return request(fnr, "/meldekortservice/api/v2/meldekort")
    }

    suspend fun hentHistoriskeMeldekort(
        fnr: Fnr,
        antallMeldeperioder: Int = 10,
    ): Either<ArenaMeldekortServiceFeil, ArenaMeldekortResponse> {
        return request(fnr, "/meldekortservice/api/v2/historiskemeldekort?antallMeldeperioder=$antallMeldeperioder")
    }

    private suspend inline fun <reified ResponseType> request(
        fnr: Fnr,
        path: String,
    ): Either<ArenaMeldekortServiceFeil, ResponseType> {
        return Either.catch {
            val token = texasClient.getSystemToken(audience, TexasIdentityProvider.AZUREAD)

            val response = httpClient.get("$baseUrl/$path") {
                accept(ContentType.Application.Json)
                bearerAuth(token.token)
                header("ident", fnr.verdi)
            }

            val status = response.status

            if (status != HttpStatusCode.OK) {
                logger.error { "Feil-response ved kall til meldekort-api - $status" }
                return ArenaMeldekortServiceFeil.FeilResponse(status).left()
            }

            return response.body<ResponseType>().right()
        }.getOrElse {
            logger.error(it) { "Ukjent feil ved request fra arena meldekortservice - ${it.message}" }
            ArenaMeldekortServiceFeil.UkjentFeil.left()
        }
    }
}

sealed interface ArenaMeldekortServiceFeil {
    data class FeilResponse(val status: HttpStatusCode) : ArenaMeldekortServiceFeil
    data object UkjentFeil : ArenaMeldekortServiceFeil
}
