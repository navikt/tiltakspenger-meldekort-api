package no.nav.tiltakspenger

import no.nav.tiltakspenger.generators.FnrGenerator
import no.nav.tiltakspenger.generators.JournalpostIdGenerator
import no.nav.tiltakspenger.generators.JournalpostIdGeneratorSerial
import no.nav.tiltakspenger.generators.SaksnummerGeneratorForTest
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import java.time.Clock

/**
 * Test-kontekst som bruker ekte Postgres via testcontainers.
 * Repos bruker ekte implementasjoner mot databasen, mens eksterne klienter er fakes.
 *
 * [saksnummergenerator], [fnrGenerator] og [journalpostIdGenerator] injiseres fra et høyere nivå
 * (test-db-manageren) slik at tester som deler samme test-db får unike id-er uten global tilstand
 * i generatorene.
 */
class TestApplicationContextMedPostgres(
    clock: Clock,
    override val texasClient: TexasClient,
    override val sessionFactory: PostgresSessionFactory,
    saksnummergenerator: SaksnummerGeneratorForTest = SaksnummerGeneratorForTest(),
    fnrGenerator: FnrGenerator = FnrGenerator(),
    journalpostIdGenerator: JournalpostIdGenerator = JournalpostIdGeneratorSerial(),
) : TestApplicationContext(clock, saksnummergenerator, fnrGenerator, journalpostIdGenerator)
