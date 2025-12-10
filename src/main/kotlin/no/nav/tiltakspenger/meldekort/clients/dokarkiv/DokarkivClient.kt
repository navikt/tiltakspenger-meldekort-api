package no.nav.tiltakspenger.meldekort.clients.dokarkiv

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId

interface DokarkivClient {
    suspend fun journalførMeldekort(
        request: JournalpostRequest,
        meldekort: Meldekort,
        callId: CorrelationId,
    ): JournalpostId
}
