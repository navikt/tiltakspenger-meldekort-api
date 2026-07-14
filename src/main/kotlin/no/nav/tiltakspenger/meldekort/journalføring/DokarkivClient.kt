package no.nav.tiltakspenger.meldekort.journalføring

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import java.time.Clock

interface DokarkivClient {
    suspend fun journalførMeldekort(
        meldekort: BrukersMeldekort,
        pdfOgJson: PdfOgJson,
        callId: CorrelationId,
        clock: Clock,
        pdfgenrs: Boolean = false,
    ): JournalpostId
}
