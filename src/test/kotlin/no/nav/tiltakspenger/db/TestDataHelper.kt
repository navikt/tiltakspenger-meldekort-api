package no.nav.tiltakspenger.db

import mu.KotlinLogging
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.meldekort.repository.MeldekortPostgresRepo
import no.nav.tiltakspenger.meldekort.repository.MeldeperiodePostgresRepo
import javax.sql.DataSource

internal class TestDataHelper(
    dataSource: DataSource,
) {
    private val log = KotlinLogging.logger {}
    private val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)
    val meldeperiodeRepo = MeldeperiodePostgresRepo(sessionFactory)
    val meldekortPostgresRepo = MeldekortPostgresRepo(sessionFactory)
}

private val dbManager = TestDatabaseManager()

/**
 * @param runIsolated Tømmer databasen før denne testen for kjøre i isolasjon. Brukes når man gjør operasjoner på tvers av saker.
 */
internal fun withMigratedDb(runIsolated: Boolean = true, test: (TestDataHelper) -> Unit) {
    dbManager.withMigratedDb(runIsolated, test)
}
