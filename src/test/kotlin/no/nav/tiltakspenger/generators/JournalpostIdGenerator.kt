package no.nav.tiltakspenger.generators

import arrow.atomic.AtomicLong
import no.nav.tiltakspenger.meldekort.journalføring.JournalpostId
import java.util.UUID

interface JournalpostIdGenerator {
    fun generer(): JournalpostId
}

/**
 * Trådsikker, deterministisk journalpost-id-generator for tester.
 *
 * Genererer sekvensielle id-er.
 * Deles på et høyere nivå (se [no.nav.tiltakspenger.db.IdGenerators] som holdes av test-db-manageren), på samme måte som de andre generatorene, slik at journalførte meldekort i samme test-db får unike og forutsigbare journalpost-id-er.
 */
class JournalpostIdGeneratorSerial(
    første: Long = 1,
) : JournalpostIdGenerator {
    private val neste = AtomicLong(første)

    override fun generer(): JournalpostId = JournalpostId(neste.getAndIncrement().toString())
}

/** Tilfeldige id-er for lokal kjøring, for å hindre kollisjoner i lokal db. */
class JournalpostIdGeneratorRandom : JournalpostIdGenerator {
    override fun generer(): JournalpostId = JournalpostId(UUID.randomUUID().toString())
}
