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
                "ELECTOR_PATH" to System.getenv("ELECTOR_PATH"),
                "KAFKA_BROKERS" to System.getenv("KAFKA_BROKERS"),
                "KAFKA_TRUSTSTORE_PATH" to System.getenv("KAFKA_TRUSTSTORE_PATH"),
                "KAFKA_KEYSTORE_PATH" to System.getenv("KAFKA_KEYSTORE_PATH"),
                "KAFKA_CREDSTORE_PASSWORD" to System.getenv("KAFKA_CREDSTORE_PASSWORD"),
                "VARSEL_HENDELSE_TOPIC" to "min-side.aapen-brukervarsel-v1",
            ),
        )

    private val prodProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.PROD.toString(),
                "SAKSBEHANDLING_API_AUDIENCE" to "api://prod-gcp.tpts.tiltakspenger-saksbehandling-api/.default",
                "SAKSBEHANDLING_API_URL" to "http://tiltakspenger-saksbehandling-api",
            ),
        )

    private val devProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.DEV.toString(),
                "SAKSBEHANDLING_API_AUDIENCE" to "api://dev-gcp.tpts.tiltakspenger-saksbehandling-api/.default",
                "SAKSBEHANDLING_API_URL" to "http://tiltakspenger-saksbehandling-api",
            ),
        )

    private val localProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.LOCAL.toString(),
                "application.httpPort" to 8083.toString(),
                "SAKSBEHANDLING_API_URL" to "http://localhost:8080",
                "logback.configurationFile" to "logback.local.xml",
                "DB_JDBC_URL" to "jdbc:postgresql://localhost:5435/meldekort?user=postgres&password=test",
                "NAIS_TOKEN_ENDPOINT" to "http://localhost:7164/api/v1/token",
                "NAIS_TOKEN_INTROSPECTION_ENDPOINT" to "http://localhost:7164/api/v1/introspect",
                "KAFKA_BROKERS" to "",
                "KAFKA_TRUSTSTORE_PATH" to "",
                "KAFKA_KEYSTORE_PATH" to "",
                "KAFKA_CREDSTORE_PASSWORD" to "",
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
    val naisTokenIntrospectionEndpoint: String by lazy {
        config()[
            Key(
                "NAIS_TOKEN_INTROSPECTION_ENDPOINT",
                stringType,
            ),
        ]
    }

    val saksbehandlingApiAudience: String by lazy { config()[Key("SAKSBEHANDLING_API_AUDIENCE", stringType)] }
    val saksbehandlingApiUrl: String by lazy { config()[Key("SAKSBEHANDLING_API_URL", stringType)] }

    val kafkaBrokers: String by lazy { config()[Key("KAFKA_BROKERS", stringType)] }
    val kafkaTruststorePath: String by lazy { config()[Key("KAFKA_TRUSTSTORE_PATH", stringType)] }
    val kafkaKeystorePath: String by lazy { config()[Key("KAFKA_KEYSTORE_PATH", stringType)] }
    val kafkaCredstorePassword: String by lazy { config()[Key("KAFKA_CREDSTORE_PASSWORD", stringType)] }

    val varselHendelseTopic: String by lazy { config()[Key("VARSEL_HENDELSE_TOPIC", stringType)] }

    fun logbackConfigurationFile() = config()[Key("logback.configurationFile", stringType)]

    fun httpPort() = config()[Key("application.httpPort", intType)]

    fun database() =
        config()[Key("DB_JDBC_URL", stringType)]

    fun isNais() = applicationProfile() != Profile.LOCAL

    fun electorPath(): String = config()[Key("ELECTOR_PATH", stringType)]
}
