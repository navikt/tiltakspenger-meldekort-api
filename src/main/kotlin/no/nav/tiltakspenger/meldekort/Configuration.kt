package no.nav.tiltakspenger.meldekort

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType


enum class Profile {
    LOCAL,
    DEV,
    PROD,
}

object Configuration {
    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "application.httpPort" to 8083.toString(),
            ),
        )

    private val localProperties =
        ConfigurationMap(
            mapOf(
                "DB_JDBC_URL" to "jdbc:postgresql://host.docker.internal:5435/meldekort?user=postgres&password=test",
            )
        )


    fun applicationProfile() =
        when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
            "dev-gcp" -> Profile.DEV
            "prod-gcp" -> Profile.PROD
            else -> Profile.LOCAL
        }


    private fun config() =
        when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
            "dev-gcp" ->
                systemProperties() overriding defaultProperties

            "prod-gcp" ->
                systemProperties() overriding defaultProperties

            else -> {
                systemProperties() overriding localProperties overriding defaultProperties
            }
        }

    fun httpPort() = config()[Key("application.httpPort", intType)]

    fun database() =
        config()[Key("DB_JDBC_URL", stringType)]

}
