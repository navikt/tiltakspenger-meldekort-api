package no.nav.tiltakspenger.meldekort.journalføring

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import java.time.Clock

class JournalførMeldekortService(
    private val journalføringRepo: JournalføringRepo,
    private val pdfgenClient: PdfgenClient,
    private val dokarkivClient: DokarkivClient,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger { }

    suspend fun journalførNyeMeldekort() {
        Either.catch {
            journalføringRepo.hentDeSomSkalJournalføres().forEach { meldekort ->
                val saksnummer = meldekort.meldeperiode.saksnummer

                log.info { "Journalfører meldekort. Saksnummer: $saksnummer, sakId: ${meldekort.sakId}, meldekortId: ${meldekort.id}" }
                Either.catch {
                    val pdfOgJson = if (meldekort.korrigering) {
                        pdfgenClient.genererKorrigertMeldekortPdf(meldekort = meldekort)
                            .getOrElse { return@forEach }
                    } else {
                        pdfgenClient.genererMeldekortPdf(meldekort = meldekort)
                            .getOrElse { return@forEach }
                    }
                    log.info { "Pdf generert for meldekort. Saksnummer: $saksnummer, sakId: ${meldekort.sakId}, meldekortId: ${meldekort.id}" }
                    val journalpostId = dokarkivClient.journalførMeldekort(
                        meldekort = meldekort,
                        pdfOgJson = pdfOgJson.first,
                        callId = CorrelationId.generate(),
                        clock = clock,
                    )
                    /*
                        TODO - fjern denne bolken, og returner kun 1 pdf når vi har verifisert at pdf'ene er lik.
                     */
                    pdfOgJson.second?.let {
                        dokarkivClient.journalførMeldekort(
                            meldekort = meldekort,
                            pdfOgJson = it,
                            callId = CorrelationId.generate(),
                            clock = clock,
                            pdfgenrs = true,
                        )
                    }

                    log.info { "Meldekort journalført. Saksnummer: $saksnummer, sakId: ${meldekort.sakId}, meldekortId: ${meldekort.id}. JournalpostId: $journalpostId" }
                    journalføringRepo.markerJournalført(meldekort.id, journalpostId, nå(clock))
                    log.info { "Meldekort markert som journalført. Saksnummer: $saksnummer, sakId: ${meldekort.sakId}, meldekortId: ${meldekort.id}. JournalpostId: $journalpostId" }
                }.onLeft {
                    log.error(it) { "Ukjent feil skjedde under generering av brev og journalføring av meldekort. Saksnummer: $saksnummer, sakId: ${meldekort.sakId}, meldekortId: ${meldekort.id}" }
                }
            }
        }.onLeft {
            log.error(it) { "Ukjent feil skjedde under journalføring av meldekort." }
        }
    }
}
