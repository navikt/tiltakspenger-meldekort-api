package no.nav.tiltakspenger.meldekort.api.db

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.api.Configuration.database

private val LOG = KotlinLogging.logger {}

object DataSource {
    private val config = database()
    private const val MAX_POOLS = 3
    const val FAIL_TIMEOUT = 5000

    private fun init(): HikariDataSource {
        LOG.info {
            "Kobler til postgress '${config.brukernavn}:XXX@" +
                "${config.host}:${config.port}/${config.database}"
        }

        return HikariDataSource().apply {
            dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
            addDataSourceProperty("serverName", config.host)
            addDataSourceProperty("portNumber", config.port)
            addDataSourceProperty("databaseName", config.database)
            addDataSourceProperty("user", config.brukernavn)
            addDataSourceProperty("password", config.passord)
            initializationFailTimeout = FAIL_TIMEOUT.toLong()
            maximumPoolSize = MAX_POOLS
        }
    }

    val hikariDataSource: HikariDataSource by lazy {
        init()
    }
}
