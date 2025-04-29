package no.nav.tiltakspenger.meldekort.clients.arena

import arrow.core.Either
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
import no.nav.tiltakspenger.meldekort.clients.httpClientWithRetry
import no.nav.tiltakspenger.meldekort.clients.texas.TexasClient

class ArenaMeldekortApiClient(
    private val baseUrl: String,
    private val audience: String,
    private val httpClient: HttpClient = httpClientWithRetry(),
    private val texasClient: TexasClient,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentPersonStatus(brukerToken: String): Either<ArenaMeldekortApiFeil, PersonStatusResponse> {
        return Either.catch {
            val response = httpClient.get("$baseUrl/person/status") {
                accept(ContentType.Application.Json)
                bearerAuth(token = brukerToken)
            }

            val status = response.status

            if (status != HttpStatusCode.OK) {
                logger.error { "Feilresponse ved kall til meldekort-api $status" }
                return ArenaMeldekortApiFeil.PersonStatusFeil.left()
            }

            return response.body<PersonStatusResponse>().right()
        }.mapLeft {
            logger.error { "Feil ved henting av person status fra arena meldekort $it" }
            return ArenaMeldekortApiFeil.PersonStatusFeil.left()
        }
    }
}

data class PersonStatusResponse(
    val id: String,
    val statusArbeidsoker: String,
    val statusYtelse: String,
)

enum class ArenaMeldekortApiFeil {
    PersonStatusFeil,
}
