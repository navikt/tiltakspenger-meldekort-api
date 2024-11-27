package no.nav.tiltakspenger.meldekort.routes

import arrow.core.Either
import io.ktor.server.application.ApplicationCall
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
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

internal suspend inline fun ApplicationCall.withFnr(
    crossinline onRight: suspend (Fnr) -> Unit,
) {
    withValidParam(
        paramName = "fnr",
        parse = Fnr::fromString,
        errorMessage = "Ugyldig fnr",
        errorCode = "ugyldig_fnr",
        onSuccess = onRight,
    )
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