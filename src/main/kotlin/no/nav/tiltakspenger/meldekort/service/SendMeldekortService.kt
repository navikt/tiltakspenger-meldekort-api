package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.SaksbehandlingClient
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SendMeldekortService(
    private val meldekortService: MeldekortService,
    private val saksbehandlingClient: SaksbehandlingClient,
) {
    private val logger = KotlinLogging.logger {}

    /** Ment å kalles fra en jobb - sender alle usendte meldekort til saksbehandling. */
    suspend fun sendMeldekort() {
        Either.catch {
            meldekortService.hentMeldekortSomSkalSendesTilSaksbehandling().forEach { meldekort ->
                logger.info { "Sender meldekort med id ${meldekort.id}" }

                Either.catch {
                    saksbehandlingClient.sendMeldekort(meldekort).getOrElse {
                        logger.warn { "Feil under sending av meldekort med id: ${meldekort.id} til SaksbehandlingApi" }
                        return@forEach
                    }
                    logger.info { "Meldekort sendt til saksbehandling: ${meldekort.id}" }
                    meldekortService.markerSendtTilSaksbehandling(
                        id = meldekort.id,
                        sendtTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS),
                    )
                    logger.info { "Meldekort oppdatert med innsendingstidspunkt ${meldekort.id}" }
                }.onLeft {
                    logger.error(it) { "Feil ved sending av meldekort: ${meldekort.id}" }
                }
            }
        }.onLeft {
            logger.error(it) { "Ukjent feil skjedde under sending av meldekort." }
        }
    }
}
