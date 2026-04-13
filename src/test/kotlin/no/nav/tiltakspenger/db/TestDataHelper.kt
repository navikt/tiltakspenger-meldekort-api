package no.nav.tiltakspenger.db

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.meldekort.repository.MeldekortPostgresRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.meldekort.repository.SakPostgresRepo
import java.time.Clock
import javax.sql.DataSource

class TestDataHelper(
    dataSource: DataSource,
    clock: Clock,
) {
    private val log = KotlinLogging.logger {}
    private val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)
    val meldeperiodeRepo = MeldeperiodePostgresRepo(sessionFactory)
    val meldekortPostgresRepo = MeldekortPostgresRepo(sessionFactory, clock)
    val sakPostgresRepo = SakPostgresRepo(sessionFactory, clock)
}

private val testDatabaseManager = TestDatabaseManager()

/**
 * @param runIsolated Tømmer databasen før denne testen for kjøre i isolasjon. Brukes når man gjør operasjoner på tvers av saker.
 */
fun withMigratedDb(runIsolated: Boolean = true, clock: Clock = TikkendeKlokke(), test: (TestDataHelper) -> Unit) {
    testDatabaseManager.withMigratedDbTestDataHelper(runIsolated = runIsolated, clock = clock, test = test)
}
