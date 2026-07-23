package no.nav.tiltakspenger.fakes.clients

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.generators.JournalpostIdGenerator
import no.nav.tiltakspenger.generators.JournalpostIdGeneratorSerial
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.meldekort.journalføring.DokarkivClient
import no.nav.tiltakspenger.meldekort.journalføring.JournalpostId
import no.nav.tiltakspenger.meldekort.journalføring.PdfOgJson
import no.nav.tiltakspenger.meldekort.meldekort.BrukersMeldekort
import org.slf4j.LoggerFactory
import java.time.Clock

class DokarkivClientFake(
    private val journalpostIdGenerator: JournalpostIdGenerator = JournalpostIdGeneratorSerial(),
) : DokarkivClient {
    private val log = LoggerFactory.getLogger(this::class.java)

    override suspend fun journalførMeldekort(
        meldekort: BrukersMeldekort,
        pdfOgJson: PdfOgJson,
        callId: CorrelationId,
        clock: Clock,
        pdfgenrs: Boolean,
    ): Either<HttpKlientError, JournalpostId> {
        return journalpostIdGenerator.generer().also {
            log.info("Fake journalføring av meldekort ${meldekort.id}, returnerer journalpostId=$it")
        }.right()
    }
}
