package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.meldekort.clients.saksbehandling.SaksbehandlingClient
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SendMeldekortService(
    private val meldekortService: MeldekortService,
    private val saksbehandlingClient: SaksbehandlingClient,
) {
    private val log = KotlinLogging.logger {}

    /** Ment å kalles fra en jobb - sender alle usendte meldekort til saksbehandling. */
    suspend fun sendMeldekort(
        correlationId: CorrelationId,
    ) {
        Either.catch {
            meldekortService.hentMeldekortSomSkalSendesTilSaksbehandling().forEach { meldekort ->
                log.info { "Sender meldekort med id ${meldekort.id}" }
                Either.catch {
                    saksbehandlingClient.sendMeldekort(meldekort, correlationId)
                    log.info { "Meldekort sendt til saksbehandling: ${meldekort.id}" }
                    meldekortService.markerSendt(meldekort.id, MeldekortStatus.Innsendt, LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                    log.info { "Meldekort oppdatert med innsendingstidspunkt ${meldekort.id}" }
                }.onLeft {
                    log.error(it) { "Feil ved sending av meldekort: ${meldekort.id}" }
                }
            }
        }.onLeft {
            log.error(RuntimeException("Trigger stacktrace for enklere debug.")) { "Ukjent feil skjedde under sending av meldekort." }
            sikkerlogg.error(it) { "Ukjent feil skjedde under sending av meldekort." }
        }
    }
}
