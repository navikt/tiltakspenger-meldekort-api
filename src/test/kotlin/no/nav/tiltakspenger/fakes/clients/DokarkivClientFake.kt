package no.nav.tiltakspenger.fakes.clients

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.meldekort.journalføring.DokarkivClient
import no.nav.tiltakspenger.meldekort.journalføring.JournalpostId
import no.nav.tiltakspenger.meldekort.journalføring.PdfOgJson
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import org.slf4j.LoggerFactory
import java.time.Clock

class DokarkivClientFake : DokarkivClient {
    private val log = LoggerFactory.getLogger(this::class.java)

    override suspend fun journalførMeldekort(
        meldekort: BrukersMeldekort,
        pdfOgJson: PdfOgJson,
        callId: CorrelationId,
        clock: Clock,
    ): JournalpostId {
        return JournalpostId("fake_journalpost_id_fra_DokarkivClientFake.kt").also {
            log.info("Fake journalføring av meldekort ${meldekort.id}, returnerer journalpostId=$it")
        }
    }
}
