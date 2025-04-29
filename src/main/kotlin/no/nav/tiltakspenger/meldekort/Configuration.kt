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

private const val APPLICATION_NAME = "tiltakspenger-meldekort-api"
const val KAFKA_CONSUMER_GROUP_ID = "$APPLICATION_NAME-consumer"

object Configuration {
    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "application.httpPort" to 8080.toString(),
                "AZURE_APP_CLIENT_ID" to System.getenv("AZURE_APP_CLIENT_ID"),
                "AZURE_APP_CLIENT_SECRET" to System.getenv("AZURE_APP_CLIENT_SECRET"),
                "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to System.getenv("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
                "logback.configurationFile" to "logback.xml",
                "SAKSBEHANDLING_API_AUDIENCE" to "tiltakspenger-saksbehandling-api",
                "DB_JDBC_URL" to System.getenv("DB_JDBC_URL"),
                "DOKARKIV_SCOPE" to "localhost",
                "DOKARKIV_URL" to "http://host.docker.internal:8091",
                "NAIS_TOKEN_ENDPOINT" to System.getenv("NAIS_TOKEN_ENDPOINT"),
                "NAIS_TOKEN_INTROSPECTION_ENDPOINT" to System.getenv("NAIS_TOKEN_INTROSPECTION_ENDPOINT"),
                "NAIS_TOKEN_EXCHANGE_ENDPOINT" to System.getenv("NAIS_TOKEN_EXCHANGE_ENDPOINT"),
                "ELECTOR_PATH" to System.getenv("ELECTOR_PATH"),
                "KAFKA_BROKERS" to System.getenv("KAFKA_BROKERS"),
                "KAFKA_TRUSTSTORE_PATH" to System.getenv("KAFKA_TRUSTSTORE_PATH"),
                "KAFKA_KEYSTORE_PATH" to System.getenv("KAFKA_KEYSTORE_PATH"),
                "PDFGEN_SCOPE" to "localhost",
                "PDFGEN_URL" to "http://host.docker.internal:8081",
                "KAFKA_CREDSTORE_PASSWORD" to System.getenv("KAFKA_CREDSTORE_PASSWORD"),
                "VARSEL_HENDELSE_TOPIC" to "min-side.aapen-brukervarsel-v1",
                "IDENTHENDELSE_TOPIC" to "tpts.identhendelse-v1",
            ),
        )

    private val prodProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.PROD.toString(),
                "DOKARKIV_SCOPE" to "api://prod-fss.teamdokumenthandtering.dokarkiv/.default",
                "DOKARKIV_URL" to "https://dokarkiv.prod-fss-pub.nais.io",
                "SAKSBEHANDLING_API_AUDIENCE" to "api://prod-gcp.tpts.tiltakspenger-saksbehandling-api/.default",
                "SAKSBEHANDLING_API_URL" to "http://tiltakspenger-saksbehandling-api",
                "MELDEKORT_FRONTEND_URL" to "https://www.nav.no/tiltakspenger/meldekort",
                "PDFGEN_SCOPE" to "api://prod-gcp.tpts.tiltakspenger-pdfgen/.default",
                "PDFGEN_URL" to "http://tiltakspenger-pdfgen",
                "ARENA_MELDEKORT_API_URL" to "https://meldekort-api.nav.no/meldekort/meldekort-api/api",
                "ARENA_MELDEKORT_API_AUDIENCE" to "prod-gcp:meldekort:meldekort-api",
            ),
        )

    private val devProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.DEV.toString(),
                "DOKARKIV_SCOPE" to "api://dev-fss.teamdokumenthandtering.dokarkiv/.default",
                "DOKARKIV_URL" to "https://dokarkiv-q2.dev-fss-pub.nais.io",
                "SAKSBEHANDLING_API_AUDIENCE" to "api://dev-gcp.tpts.tiltakspenger-saksbehandling-api/.default",
                "SAKSBEHANDLING_API_URL" to "http://tiltakspenger-saksbehandling-api",
                "MELDEKORT_FRONTEND_URL" to "https://www.ansatt.dev.nav.no/tiltakspenger/meldekort",
                "PDFGEN_SCOPE" to "api://dev-gcp.tpts.tiltakspenger-pdfgen/.default",
                "PDFGEN_URL" to "http://tiltakspenger-pdfgen",
                "ARENA_MELDEKORT_API_URL" to "https://meldekort-api-q2.intern.dev.nav.no/meldekort/meldekort-api/api",
                "ARENA_MELDEKORT_API_AUDIENCE" to "dev-gcp:meldekort:meldekort-api-q2",
            ),
        )

    private val localProperties =
        ConfigurationMap(
            mapOf(
                "application.profile" to Profile.LOCAL.toString(),
                "application.httpPort" to 8083.toString(),
                "AZURE_APP_CLIENT_ID" to "tiltakspenger-saksbehandling-api",
                "AZURE_APP_CLIENT_SECRET" to "secret",
                "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "http://host.docker.internal:6969/azuread/token",
                "DOKARKIV_SCOPE" to "localhost",
                "DOKARKIV_URL" to "http://host.docker.internal:8091",
                "SAKSBEHANDLING_API_URL" to "http://localhost:8080",
                "logback.configurationFile" to "logback.local.xml",
                "DB_JDBC_URL" to "jdbc:postgresql://localhost:5435/meldekort?user=postgres&password=test",
                "NAIS_TOKEN_ENDPOINT" to "http://localhost:7164/api/v1/token",
                "NAIS_TOKEN_INTROSPECTION_ENDPOINT" to "http://localhost:7164/api/v1/introspect",
                "NAIS_TOKEN_EXCHANGE_ENDPOINT" to "http://localhost:7164/api/v1/token/exchange",
                "KAFKA_BROKERS" to "",
                "KAFKA_TRUSTSTORE_PATH" to "",
                "KAFKA_KEYSTORE_PATH" to "",
                "KAFKA_CREDSTORE_PASSWORD" to "",
                "MELDEKORT_FRONTEND_URL" to "http://localhost:2223/tiltakspenger/meldekort",
                "PDFGEN_SCOPE" to "localhost",
                "PDFGEN_URL" to "http://host.docker.internal:8081",
                "ARENA_MELDEKORT_API_URL" to "http://host.docker.internal:8091/meldekort/meldekort-api/api",
                "ARENA_MELDEKORT_API_AUDIENCE" to "arena-meldekort-api",
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
    val naisTokenExchangeEndpoint: String by lazy { config()[Key("NAIS_TOKEN_EXCHANGE_ENDPOINT", stringType)] }

    /** Samme som hvis man gjør en get til AZURE_APP_WELL_KNOWN_URL og plukker ut 'token_endpoint' */
    val azureOpenidConfigTokenEndpoint: String by lazy { config()[Key("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT", stringType)] }
    val azureClientId: String by lazy { config()[Key("AZURE_APP_CLIENT_ID", stringType)] }
    val azureClientSecret: String by lazy { config()[Key("AZURE_APP_CLIENT_SECRET", stringType)] }

    val saksbehandlingApiAudience: String by lazy { config()[Key("SAKSBEHANDLING_API_AUDIENCE", stringType)] }
    val saksbehandlingApiUrl: String by lazy { config()[Key("SAKSBEHANDLING_API_URL", stringType)] }

    val dokarkivUrl: String by lazy { config()[Key("DOKARKIV_URL", stringType)] }
    val dokarkivScope: String by lazy { config()[Key("DOKARKIV_SCOPE", stringType)] }
    val pdfgenUrl: String by lazy { config()[Key("PDFGEN_URL", stringType)] }
    val varselHendelseTopic: String by lazy { config()[Key("VARSEL_HENDELSE_TOPIC", stringType)] }

    val identhendelseTopic: String by lazy { config()[Key("IDENTHENDELSE_TOPIC", stringType)] }

    val meldekortFrontendUrl: String by lazy { config()[Key("MELDEKORT_FRONTEND_URL", stringType)] }

    val arenaMeldekortApiUrl: String by lazy { config()[Key("ARENA_MELDEKORT_API_URL", stringType)] }
    val arenaMeldekortApiAudience: String by lazy { config()[Key("ARENA_MELDEKORT_API_AUDIENCE", stringType)] }

    fun logbackConfigurationFile() = config()[Key("logback.configurationFile", stringType)]

    fun httpPort() = config()[Key("application.httpPort", intType)]

    fun database() =
        config()[Key("DB_JDBC_URL", stringType)]

    fun isNais() = applicationProfile() != Profile.LOCAL

    fun electorPath(): String = config()[Key("ELECTOR_PATH", stringType)]
}
