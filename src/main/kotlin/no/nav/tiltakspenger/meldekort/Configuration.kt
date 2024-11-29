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
                "application.httpPort" to 8080.toString(),
                "logback.configurationFile" to "logback.xml",
                "SAKSBEHANDLING_API_AUDIENCE" to "tiltakspenger-saksbehandling-api",
                "DB_JDBC_URL" to System.getenv("DB_JDBC_URL"),
                "NAIS_TOKEN_ENDPOINT" to System.getenv("NAIS_TOKEN_ENDPOINT"),
                "NAIS_TOKEN_INTROSPECTION_ENDPOINT" to System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT"),
            ),
        )

    private val prodProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.PROD.toString(),
                "SAKSBEHANDLING_API_URL" to "https://tiltakspenger-meldekort-api.intern.nav.no",
            ),
        )

    private val devProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.DEV.toString(),
                "SAKSBEHANDLING_API_URL" to "https://tiltakspenger-meldekort-api.intern.dev.nav.no",
            ),
        )

    private val localProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.LOCAL.toString(),
                "application.httpPort" to 8083.toString(),
                "SAKSBEHANDLING_API_URL" to "http://localhost8080",
                "logback.configurationFile" to "logback.local.xml",
                "DB_JDBC_URL" to "jdbc:postgresql://localhost:5435/meldekort?user=postgres&password=test",
                "NAIS_TOKEN_ENDPOINT" to "Denne finnes bare runtime i miljø",
                "NAIS_TOKEN_INTROSPECTION_ENDPOINT" to "Denne finnes bare runtime i miljø",
            ),
        )

    fun applicationProfile() =
        when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
            "prod-gcp" -> Profile.PROD
            "dev-gcp" -> Profile.DEV
            else -> Profile.LOCAL
        }

    private fun config() =
        when (applicationProfile()) {
            Profile.PROD ->
                systemProperties() overriding prodProperties overriding defaultProperties

            Profile.DEV ->
                systemProperties() overriding devProperties overriding defaultProperties

            Profile.LOCAL -> {
                systemProperties() overriding localProperties overriding defaultProperties
            }
        }

    val naisTokenEndpoint: String by lazy { config()[Key("NAIS_TOKEN_ENDPOINT", stringType)] }
    val naisTokenIntrospectionEndpoint: String by lazy { config()[Key("NAIS_TOKEN_INTROSPECTION_ENDPOINT", stringType)] }

    val saksbehandlingApiAudience: String by lazy { config()[Key("SAKSBEHANDLING_API_AUDIENCE", stringType)] }
    val saksbehandlingApiUrl: String by lazy { config()[Key("SAKSBEHANDLING_API_URL", stringType)] }

    fun logbackConfigurationFile() = config()[Key("logback.configurationFile", stringType)]

    fun httpPort() = config()[Key("application.httpPort", intType)]

    fun database() =
        config()[Key("DB_JDBC_URL", stringType)]
}
