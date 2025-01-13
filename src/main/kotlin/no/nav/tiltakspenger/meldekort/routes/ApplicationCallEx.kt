package no.nav.tiltakspenger.meldekort.routes

import arrow.core.Either
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.authorization
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest

private val logger = KotlinLogging.logger {}

internal suspend inline fun ApplicationCall.withMeldekortId(
    crossinline onRight: suspend (HendelseId) -> Unit,
) {
    withValidParam(
        paramName = "meldekortId",
        parse = HendelseId::fromString,
        errorMessage = "Ugyldig meldekort id",
        errorCode = "ugyldig_meldekort_id",
        onSuccess = onRight,
    )
}

fun ApplicationCall.bearerToken(): String? =
    request.authorization()
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
        ?.removePrefix("Bearer ")

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
