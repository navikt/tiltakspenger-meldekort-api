package no.nav.tiltakspenger.meldekort.clients.dokarkiv

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.journalføring.JournalpostId
import org.slf4j.LoggerFactory

class DokarkivClientFake : DokarkivClient {
    private val log = LoggerFactory.getLogger(this::class.java)

    override suspend fun journalførMeldekort(
        request: JournalpostRequest,
        meldekort: Meldekort,
        callId: CorrelationId,
    ): JournalpostId {
        return JournalpostId("fake_journalpost_id_fra_DokarkivClientFake.kt").also {
            log.info("Fake journalføring av meldekort ${meldekort.id}, returnerer journalpostId=$it")
        }
    }
}
