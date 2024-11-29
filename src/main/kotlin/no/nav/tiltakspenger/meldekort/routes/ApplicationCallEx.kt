package no.nav.tiltakspenger.meldekort.routes

import arrow.core.Either
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.authorization
import io.ktor.server.request.host
import io.ktor.server.request.uri
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest

private val logger = KotlinLogging.logger {}

internal suspend inline fun ApplicationCall.withMeldekortId(
    crossinline onRight: suspend (MeldekortId) -> Unit,
) {
    withValidParam(
        paramName = "meldekortId",
        parse = MeldekortId::fromString,
        errorMessage = "Ugyldig meldekort id",
        errorCode = "ugyldig_meldekort_id",
        onSuccess = onRight,
    )
}

fun ApplicationCall.bearerToken(): String? =
    request.authorization()
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.removePrefix("Bearer ")

// loginUrl constructs a URL string that points to the login endpoint (Wonderwall) for redirecting a request.
// It also indicates that the user should be redirected back to the original request path after authentication
internal fun ApplicationCall.loginUrl(defaultHost: String): String {
    val host = defaultHost.ifEmpty(defaultValue = {
        "${this.request.local.scheme}://${this.request.host()}"
    })

    return "$host/oauth2/login?redirect=${this.request.uri}"
}

private suspend inline fun <T> ApplicationCall.withValidParam(
    paramName: String,
    parse: (String) -> T,
    errorMessage: String,
    errorCode: String,
    crossinline onSuccess: suspend (T) -> Unit,
) {
    Either.catch {
        parse(this.parameters[paramName]!!)
    }.fold(
        ifLeft = {
            logger.debug(it) { "Feil ved parsing av parameter $paramName. errorMessage: $errorMessage, errorCode: $errorCode" }
            this.respond400BadRequest(
                melding = errorMessage,
                kode = errorCode,
            )
        },
        ifRight = { onSuccess(it) },
    )
}
