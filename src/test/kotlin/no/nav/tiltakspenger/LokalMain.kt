package no.nav.tiltakspenger

import arrow.core.flatMap
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.dev.devRoutes
import no.nav.tiltakspenger.libs.lokal.LokalPostgresConfig
import no.nav.tiltakspenger.libs.lokal.somMelding
import no.nav.tiltakspenger.libs.lokal.startLokalPostgres
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.meldekort.infra.Configuration
import no.nav.tiltakspenger.meldekort.infra.start
import java.time.Clock
import kotlin.system.exitProcess

/**
 * Starter opp serveren lokalt med postgres i docker og fakes fra [LokalApplicationContext].
 * Postgres startes for deg hvis den ikke allerede kjører — se [startLokalPostgres].
 */
fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}
    log.info { "Starter lokal server" }
    val clock = Clock.system(zoneIdOslo)

    val postgres = LokalPostgresConfig.fraJdbcUrl(
        jdbcUrl = Configuration.database(),
        composeTjeneste = "postgresMeldekort",
    ).flatMap { config ->
        startLokalPostgres(config = config, clock = clock)
    }.getOrElse { feil ->
        log.error(feil.årsak) { feil.somMelding() }
        exitProcess(1)
    }
    // Jdbc-url-en er den samme som i Configuration når vi kjører mot compose, men i testcontainers-modus er porten tilfeldig.
    System.setProperty("DB_JDBC_URL", postgres.jdbcUrl)
    log.info { "Lokal postgres er klar: ${postgres.beskrivelse}" }

    val applicationContext = LokalApplicationContext(clock)
    start(
        log = log,
        host = "127.0.0.1",
        isNais = false,
        applicationContext = applicationContext,
        // Dev-only endepunkter (f.eks. POST /dev/sak).
        // Aldri med i prod.
        additionalRoutes = { devRoutes(applicationContext) },
    )
}
