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
import io.ktor.client.statement.HttpResponse
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
        val response = request(fnr, "meldekortservice/api/v2/meldekort").getOrElse {
            return it.left()
        }

        val status = response.status

        if (status == HttpStatusCode.OK) {
            return response.body<ArenaMeldekortResponse>().right()
        }

        if (status == HttpStatusCode.NoContent) {
            return null.right()
        }

        logger.warn { "Feil-response fra meldekortservice/meldekort - $status" }
        return ArenaMeldekortServiceFeil.FeilResponse(status).left()
    }

    suspend fun hentHistoriskeMeldekort(
        fnr: Fnr,
        antallMeldeperioder: Int = 10,
    ): Either<ArenaMeldekortServiceFeil, ArenaMeldekortResponse?> {
        val response = request(fnr, "meldekortservice/api/v2/historiskemeldekort?antallMeldeperioder=$antallMeldeperioder").getOrElse {
            return it.left()
        }

        val status = response.status

        if (status == HttpStatusCode.OK) {
            return response.body<ArenaMeldekortResponse>().right()
        }

        // historiskemeldekort returnerer 503-feil dersom brukeren ikke finnes.
        // Vi kaller aldri denne uten å kalle /meldekort først, så velger å ikke bry oss om feilhåndtering her
        logger.info { "Feil-response fra meldekortservice/historiskemeldekort (dette betyr antagelig at brukeren ikke finnes) - $status" }
        return null.right()
    }

    private suspend fun request(
        fnr: Fnr,
        path: String,
    ): Either<ArenaMeldekortServiceFeil, HttpResponse> {
        return Either.catch {
            val token = getToken()

            httpClient.get("$baseUrl/$path") {
                accept(ContentType.Application.Json)
                bearerAuth(token.token)
                header("ident", fnr.verdi)
            }.right()
        }.getOrElse {
            logger.error(it) { "Feil ved request til arena meldekortservice - ${it.message}" }
            ArenaMeldekortServiceFeil.UkjentFeil.left()
        }
    }
}

sealed interface ArenaMeldekortServiceFeil {
    data class FeilResponse(val status: HttpStatusCode) : ArenaMeldekortServiceFeil
    data object UkjentFeil : ArenaMeldekortServiceFeil
}
