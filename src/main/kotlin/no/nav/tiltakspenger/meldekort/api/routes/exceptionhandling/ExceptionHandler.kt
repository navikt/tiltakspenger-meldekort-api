package no.nav.tiltakspenger.meldekort.api.routes.exceptionhandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.api.exceptions.IkkeFunnetException
import no.nav.tiltakspenger.meldekort.api.exceptions.UgyldigRequestException
import no.nav.tiltakspenger.meldekort.api.tilgang.ManglendeJWTTokenException

private val LOG = KotlinLogging.logger {}
private val SECURELOG = KotlinLogging.logger("tjenestekall")

object ExceptionHandler {
    suspend fun handle(
        call: ApplicationCall,
        cause: Throwable,
    ) {
        SECURELOG.error("Feil i route ${call.request.uri}", cause)
        LOG.error("Feil i route: ${cause.message}. Se securelog for mer detaljer. ${cause.stackTraceToString()}")
        when (cause) {
            is IllegalStateException -> {
                call.respondWith(HttpStatusCode.InternalServerError, cause)
            }

            is ManglendeJWTTokenException -> {
                call.respondWith(HttpStatusCode.Unauthorized, cause)
            }

            is UgyldigRequestException -> {
                call.respondWith(HttpStatusCode.BadRequest, cause)
            }

            is ContentTransformationException -> {
                call.respondWith(HttpStatusCode.BadRequest, cause)
            }

            is IkkeFunnetException -> {
                call.respondWith(HttpStatusCode.NotFound, cause)
            }

            // Catch all
            else -> {
                call.respondWith(HttpStatusCode.InternalServerError, cause)
            }
        }
    }

    private suspend fun ApplicationCall.respondWith(
        statusCode: HttpStatusCode,
        ex: Throwable,
    ) {
        this.respond(
            statusCode,
            ExceptionResponse(ex, statusCode),
        )
    }
}
