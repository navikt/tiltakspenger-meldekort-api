package no.nav.tiltakspenger.meldekort.journalføring

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.meldekort.meldekort.Meldekort
import java.time.Clock

interface DokarkivClient {
    suspend fun journalførMeldekort(
        meldekort: Meldekort,
        pdfOgJson: PdfOgJson,
        callId: CorrelationId,
        clock: Clock,
    ): JournalpostId
}
