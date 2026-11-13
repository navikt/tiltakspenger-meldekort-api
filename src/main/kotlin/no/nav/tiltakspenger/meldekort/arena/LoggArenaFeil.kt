package no.nav.tiltakspenger.meldekort.arena

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.attempts
import no.nav.tiltakspenger.libs.httpklient.endepunkt
import no.nav.tiltakspenger.libs.httpklient.rawRequestString
import no.nav.tiltakspenger.libs.httpklient.rawResponseString
import no.nav.tiltakspenger.libs.httpklient.throwableOrNull
import no.nav.tiltakspenger.libs.httpklient.totalDuration
import no.nav.tiltakspenger.libs.logging.Sikkerlogg

/**
 * Som [no.nav.tiltakspenger.libs.httpklient.loggFeil], men logger på WARN i stedet for ERROR.
 * Feil mot Arena (meldekortservice) håndteres ved at statusen blir ukjent, og er ikke noe vi kan eller trenger å følge opp.
 */
fun HttpKlientError.loggArenaFeil(
    logger: KLogger,
    operasjon: String,
    kontekst: String,
    sikkerlogg: Sikkerlogg,
) {
    val throwable = throwableOrNull()
    val status = metadata.statusCode?.let { "status: $it, " } ?: ""
    val start = "Feil ved $operasjon. $kontekst. $beskrivelse mot $endepunkt. ${status}forsøk: $attempts, brukt: $totalDuration."
    val logMelding = "$start ${sikkerlogg.seSikkerlogg}"
    val sikkerMelding =
        "$start Request: $rawRequestString. Response: $rawResponseString. ResponseHeaders: ${metadata.responseHeaders}."
    logger.warn(throwable) { logMelding }
    sikkerlogg.warn(throwable) { sikkerMelding }
}
