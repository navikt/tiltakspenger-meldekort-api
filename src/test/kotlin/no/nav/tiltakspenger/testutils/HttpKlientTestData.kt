package no.nav.tiltakspenger.testutils

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import java.time.Instant
import kotlin.time.Duration

// TODO jah: Dette er ikke noe som er unikt for meldekort-api. Flytt til libs. Og bytt til å bruke det. Se over andre tilfeller og alle de andre repoene også.
val testTokenProvider = object : AuthTokenProvider {
    override suspend fun hentToken(skipCache: Boolean) = AccessToken("test-token", Instant.now(fixedClock).plusSeconds(3600))
}

/** Minimal metadata til feiltyper i tester som ikke bryr seg om HTTP-detaljene. */
fun tomHttpKlientMetadata(statusCode: Int? = 200) = HttpKlientMetadata(
    rawRequestString = "{}",
    rawResponseString = "{}",
    requestHeaders = emptyMap(),
    responseHeaders = emptyMap(),
    statusCode = statusCode,
    attempts = 1,
    attemptDurations = emptyList(),
    totalDuration = Duration.ZERO,
    tidsstempler = HttpKlientTidsstempler.INGEN,
)

/** Feil-svar til fakes/tester som øver feilgrenen. */
fun uventetStatusFeil(statusCode: Int = 500, body: String = "feil fra tjenesten"): Either<HttpKlientError, Nothing> =
    HttpKlientError.UventetStatus(statusCode = statusCode, body = body, metadata = tomHttpKlientMetadata(statusCode)).left()
