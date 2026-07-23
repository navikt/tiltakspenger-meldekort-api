package no.nav.tiltakspenger.meldekort.sending

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.meldekort.jobb.JobbResultat
import no.nav.tiltakspenger.meldekort.sak.SaksbehandlingClient
import java.time.Clock
import java.time.temporal.ChronoUnit

class SendMeldekortJobb(
    private val sendMeldekortRepo: SendMeldekortRepo,
    private val saksbehandlingClient: SaksbehandlingClient,
    private val clock: Clock,
    private val sikkerlogg: Sikkerlogg = Sikkerlogg,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Ment å kalles fra en jobb - sender alle usendte meldekort til saksbehandling.
     * Returnerer [JobbResultat.IngenArbeid] når det ikke fantes usendte meldekort, slik at jobben kan melde fra om den hadde arbeid.
     */
    suspend fun sendMeldekort(): JobbResultat {
        return Either.catch {
            val usendteMeldekort = sendMeldekortRepo.hentMeldekortForSendingTilSaksbehandling()
            usendteMeldekort.forEach { meldekort ->
                logger.info { "Sender meldekort med id ${meldekort.id}" }

                Either.catch {
                    saksbehandlingClient.sendMeldekort(meldekort).getOrElse {
                        it.loggFeil(logger, "sending av meldekort til saksbehandling-api", "meldekortId: ${meldekort.id}", sikkerlogg)
                        return@forEach
                    }
                    logger.info { "Meldekort sendt til saksbehandling: ${meldekort.id}" }
                    sendMeldekortRepo.markerSendtTilSaksbehandling(
                        id = meldekort.id,
                        sendtTidspunkt = nå(clock).truncatedTo(ChronoUnit.MICROS),
                    )
                    logger.info { "Meldekort ${meldekort.id} oppdatert med innsendingstidspunkt" }
                }.onLeft {
                    logger.error(it) { "Feil ved sending av meldekort: ${meldekort.id}" }
                }
            }
            if (usendteMeldekort.isEmpty()) JobbResultat.IngenArbeid else JobbResultat.UtførteArbeid
        }.getOrElse {
            logger.error(it) { "Ukjent feil skjedde under sending av meldekort." }
            JobbResultat.Feilet
        }
    }
}
