package no.nav.tiltakspenger.meldekort.domene.journalføring

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.meldekort.clients.dokarkiv.DokarkivClient
import no.nav.tiltakspenger.meldekort.clients.dokarkiv.toJournalpostDokument
import no.nav.tiltakspenger.meldekort.clients.pdfgen.PdfgenClient
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import java.time.Clock

class JournalførMeldekortService(
    private val meldekortRepo: MeldekortRepo,
    private val pdfgenClient: PdfgenClient,
    private val dokarkivClient: DokarkivClient,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger { }

    suspend fun journalførNyeMeldekort() {
        Either.catch {
            meldekortRepo.hentDeSomSkalJournalføres().forEach { meldekort ->
                val saksnummer = meldekort.meldeperiode.saksnummer

                log.info { "Journalfører meldekort. Saksnummer: $saksnummer, sakId: ${meldekort.sakId}, meldekortId: ${meldekort.id}" }
                Either.catch {
                    val pdfOgJson =
                        pdfgenClient.genererPdf(meldekort = meldekort)
                            .getOrElse { return@forEach }
                    log.info { "Pdf generert for meldekort. Saksnummer: $saksnummer, sakId: ${meldekort.sakId}, meldekortId: ${meldekort.id}" }
                    val journalpostId = dokarkivClient.journalførMeldekort(
                        request = meldekort.toJournalpostDokument(pdfOgJson = pdfOgJson),
                        meldekort = meldekort,
                        callId = CorrelationId.generate(),
                    )
                    log.info { "Meldekort journalført. Saksnummer: $saksnummer, sakId: ${meldekort.sakId}, meldekortId: ${meldekort.id}. JournalpostId: $journalpostId" }
                    meldekortRepo.markerJournalført(meldekort.id, journalpostId, nå(clock))
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
