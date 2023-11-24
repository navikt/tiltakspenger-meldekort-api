package no.nav.tiltakspenger.meldekort.db

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

object DataSource {
    private const val MAX_POOLS = 3
    const val DB_USERNAME_KEY = "DB_USERNAME"
    const val DB_PASSWORD_KEY = "DB_PASSWORD"
    const val DB_DATABASE_KEY = "DB_DATABASE"
    const val DB_HOST_KEY = "DB_HOST"
    const val DB_PORT_KEY = "DB_PORT"
    const val FAIL_TIMEOUT = 5000

    private fun getProperty(key: String) = System.getenv(key) ?: System.getProperty(key)

    private fun init(): HikariDataSource {
        LOG.info {
            "Kobler til postgress '${getProperty(DB_USERNAME_KEY)}:XXX@" +
                "${getProperty(DB_HOST_KEY)}:${getProperty(DB_PORT_KEY)}/${getProperty(DB_DATABASE_KEY)}"
        }

        return HikariDataSource().apply {
            dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
            addDataSourceProperty("serverName", getProperty(DB_HOST_KEY))
            addDataSourceProperty("portNumber", getProperty(DB_PORT_KEY))
            addDataSourceProperty("databaseName", getProperty(DB_DATABASE_KEY))
            addDataSourceProperty("user", getProperty(DB_USERNAME_KEY))
            addDataSourceProperty("password", getProperty(DB_PASSWORD_KEY))
            initializationFailTimeout = FAIL_TIMEOUT.toLong()
            maximumPoolSize = MAX_POOLS
        }
    }

    val hikariDataSource: HikariDataSource by lazy {
        init()
    }
}
