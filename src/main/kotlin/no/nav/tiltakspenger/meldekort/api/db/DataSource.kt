package no.nav.tiltakspenger.meldekort.api.db

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import no.nav.tiltakspenger.meldekort.api.Configuration.database

private val LOG = KotlinLogging.logger {}

object DataSource {
    private const val MAX_POOLS = 3

//    const val DB_USERNAME_KEY = "DB_USERNAME"
//    const val DB_PASSWORD_KEY = "DB_PASSWORD"
//    const val DB_DATABASE_KEY = "DB_DATABASE"
//    const val DB_HOST_KEY = "DB_HOST"
//    const val DB_PORT_KEY = "DB_PORT"
    const val FAIL_TIMEOUT = 5000

//    private fun getProperty(key: String) = System.getenv(key) ?: System.getProperty(key)

    private fun init(): HikariDataSource {
        LOG.info {
            "Kobler til postgress '${database().brukernavn}:XXX@" +
                "${database().host}:${database().port}/${database().database}"
        }

        return HikariDataSource().apply {
            dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
            addDataSourceProperty("serverName", database().host)
            addDataSourceProperty("portNumber", database().port)
            addDataSourceProperty("databaseName", database().database)
            addDataSourceProperty("user", database().brukernavn)
            addDataSourceProperty("password", database().passord)
            initializationFailTimeout = FAIL_TIMEOUT.toLong()
            maximumPoolSize = MAX_POOLS
        }
    }

    val hikariDataSource: HikariDataSource by lazy {
        init()
    }
}
