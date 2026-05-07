package no.nav.tiltakspenger.meldekort.infra.db

import no.nav.tiltakspenger.meldekort.infra.Configuration
import no.nav.tiltakspenger.meldekort.infra.Profile
import org.flywaydb.core.Flyway
import javax.sql.DataSource

private fun flyway(dataSource: DataSource): Flyway =
    when (Configuration.applicationProfile()) {
        Profile.LOCAL -> localFlyway(dataSource)
        else -> gcpFlyway(dataSource)
    }

private fun localFlyway(dataSource: DataSource) =
    Flyway
        .configure()
        .loggers("slf4j")
        .encoding("UTF-8")
        .locations("db/migration", "db/local-migration")
        .dataSource(dataSource)
        .load()

private fun gcpFlyway(dataSource: DataSource) =
    Flyway
        .configure()
        .loggers("slf4j")
        .encoding("UTF-8")
        .dataSource(dataSource)
        .load()

fun flywayMigrate(dataSource: DataSource) {
    flyway(dataSource).migrate()
}
