package no.nav.tiltakspenger.db

import no.nav.tiltakspenger.generators.FnrGenerator
import no.nav.tiltakspenger.generators.JournalpostIdGenerator
import no.nav.tiltakspenger.generators.JournalpostIdGeneratorSerial
import no.nav.tiltakspenger.generators.SaksnummerGeneratorForTest

/**
 * Samler id-/nummergeneratorer som må deles på tvers av tester som kjører mot samme test-db.
 *
 * Holdes av [TestDatabaseManager] (én instans per test-db), og injiseres ned i test-konteksten.
 * På den måten unngår vi global tilstand begravd i den enkelte generatoren, samtidig som tester
 * som deler en ikke-isolert test-db får unike saksnumre, fnr og journalpost-id-er.
 */
data class IdGenerators(
    val saksnummerGenerator: SaksnummerGeneratorForTest = SaksnummerGeneratorForTest(),
    val fnrGenerator: FnrGenerator = FnrGenerator(),
    val journalpostIdGenerator: JournalpostIdGenerator = JournalpostIdGeneratorSerial(),
)
