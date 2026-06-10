package no.nav.tiltakspenger.db

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infrastruktur.SessionCounter
import no.nav.tiltakspenger.meldekort.bruker.infra.repo.BrukerSakPostgresRepo
import no.nav.tiltakspenger.meldekort.journalføring.infra.JournalføringPostgresRepo
import no.nav.tiltakspenger.meldekort.landingsside.infra.repo.LandingssidePostgresRepo
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortPostgresRepo
import no.nav.tiltakspenger.meldekort.meldeperiode.infra.MeldeperiodePostgresRepo
import no.nav.tiltakspenger.meldekort.microfrontend.infra.repo.MicrofrontendPostgresRepo
import no.nav.tiltakspenger.meldekort.mottak.infra.MottakPostgresRepo
import no.nav.tiltakspenger.meldekort.sak.infra.SakPostgresRepo
import no.nav.tiltakspenger.meldekort.sending.infra.SendMeldekortPostgresRepo
import no.nav.tiltakspenger.meldekort.varsler.infra.SakVarselPostgresRepo
import no.nav.tiltakspenger.meldekort.varsler.infra.VarselMeldekortPostgresRepo
import no.nav.tiltakspenger.meldekort.varsler.infra.VarselPostgresRepo
import java.time.Clock
import javax.sql.DataSource

class TestDataHelper(
    dataSource: DataSource,
    clock: Clock,
    private val idGenerators: IdGenerators = IdGenerators(),
) {
    private val log = KotlinLogging.logger {}
    private val sessionCounter = SessionCounter(log)
    val sessionFactory = PostgresSessionFactory(dataSource, sessionCounter)
    val meldeperiodeRepo = MeldeperiodePostgresRepo(sessionFactory)
    val varselPostgresRepo = VarselPostgresRepo(sessionFactory, clock)
    val meldekortPostgresRepo = MeldekortPostgresRepo(sessionFactory, clock)
    val sendMeldekortPostgresRepo = SendMeldekortPostgresRepo(sessionFactory)
    val journalføringPostgresRepo = JournalføringPostgresRepo(sessionFactory)
    val sakPostgresRepo = SakPostgresRepo(sessionFactory)
    val microfrontendPostgresRepo = MicrofrontendPostgresRepo(sessionFactory, clock)
    val mottakPostgresRepo = MottakPostgresRepo(sessionFactory)
    val brukerSakPostgresRepo = BrukerSakPostgresRepo(sessionFactory)
    val landingssidePostgresRepo = LandingssidePostgresRepo(sessionFactory, clock)
    val sakVarselPostgresRepo = SakVarselPostgresRepo(sessionFactory)
    val varselMeldekortPostgresRepo = VarselMeldekortPostgresRepo(sessionFactory)

    /** Deterministisk, unikt saksnummer (delt på tvers av tester i samme test-db). */
    fun nesteSaksnummer(): String = idGenerators.saksnummerGenerator.generer()

    /** Deterministisk, unikt fnr. Foretrekkes fremfor `Fnr.random()` for å unngå flaky kollisjoner. */
    fun nesteFnr() = idGenerators.fnrGenerator.generer()
}

private val testDatabaseManager = TestDatabaseManager()

/**
 * @param runIsolated Tømmer databasen før denne testen for kjøre i isolasjon. Brukes når man gjør operasjoner på tvers av saker.
 */
fun withMigratedDb(runIsolated: Boolean = true, clock: Clock = TikkendeKlokke(), test: (TestDataHelper) -> Unit) {
    testDatabaseManager.withMigratedDbTestDataHelper(runIsolated = runIsolated, clock = clock, test = test)
}
