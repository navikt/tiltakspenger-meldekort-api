package no.nav.tiltakspenger.meldekort.api

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.tiltakspenger.meldekort.api.auth.AzureTokenProvider

enum class Profile {
    LOCAL, DEV, PROD
}

object Configuration {
    fun applicationProfile() =
        when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
            "dev-gcp" -> Profile.DEV
            "prod-gcp" -> Profile.PROD
            else -> Profile.LOCAL
        }

    fun isNais() = applicationProfile() != Profile.LOCAL

    private val defaultProperties = ConfigurationMap(
        mapOf(
            "application.httpPort" to 8080.toString(),
            "AZURE_APP_CLIENT_ID" to System.getenv("AZURE_APP_CLIENT_ID"),
            "AZURE_APP_CLIENT_SECRET" to System.getenv("AZURE_APP_CLIENT_SECRET"),
            "AZURE_APP_WELL_KNOWN_URL" to System.getenv("AZURE_APP_WELL_KNOWN_URL"),
            "AZURE_OPENID_CONFIG_ISSUER" to System.getenv("AZURE_OPENID_CONFIG_ISSUER"),
            "AZURE_OPENID_CONFIG_JWKS_URI" to System.getenv("AZURE_OPENID_CONFIG_JWKS_URI"),
            "DB_DATABASE" to System.getenv("DB_DATABASE"),
            "DB_HOST" to System.getenv("DB_HOST"),
            "DB_PASSWORD" to System.getenv("DB_PASSWORD"),
            "DB_PORT" to System.getenv("DB_PORT"),
            "DB_USERNAME" to System.getenv("DB_USERNAME"),
            "logback.configurationFile" to "logback.xml",
            "ELECTOR_PATH" to System.getenv("ELECTOR_PATH"),
        ),
    )

    private val localProperties = ConfigurationMap(
        mapOf(
            "application.httpPort" to 8081.toString(),
            "application.profile" to Profile.LOCAL.toString(),
            "DB_DATABASE" to "meldekort",
            "DB_HOST" to "localhost",
            "DB_PASSWORD" to "test",
            "DB_PORT" to "5431",
            "DB_USERNAME" to "postgres",
            "logback.configurationFile" to "logback.local.xml",
            "AZURE_APP_CLIENT_ID" to "tiltakspenger-meldekort-api",
            "AZURE_APP_CLIENT_SECRET" to "secret",
            "AZURE_APP_WELL_KNOWN_URL" to "http://host.docker.internal:6969/azure/.well-known/openid-configuration",
            "AZURE_OPENID_CONFIG_ISSUER" to "http://host.docker.internal:6969/azure",
            "AZURE_OPENID_CONFIG_JWKS_URI" to "http://host.docker.internal:6969/azure/jwks",
            "SCOPE_UTBETALING" to "localhost",
            "SCOPE_DOKUMENT" to "localhost",
            "UTBETALING_URL" to "http://localhost:8083",
            "DOKUMENT_URL" to "http://localhost:8084",
            // kommentar jah: Skal ikke trenge denne lokalt
            "ELECTOR_PATH" to "http://localhost:8085",
        ),
    )

    private val devProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.DEV.toString(),
            "SCOPE_UTBETALING" to "api://dev-gcp.tpts.tiltakspenger-utbetaling/.default",
            "SCOPE_DOKUMENT" to "api://dev-gcp.tpts.tiltakspenger-dokument/.default",
            "UTBETALING_URL" to "https://tiltakspenger-utbetaling.intern.dev.nav.no",
            "DOKUMENT_URL" to "http://tiltakspenger-dokument",
        ),
    )

    private val prodProperties = ConfigurationMap(
        mapOf(
            "application.profile" to Profile.PROD.toString(),
            "SCOPE_UTBETALING" to "api://prod-gcp.tpts.tiltakspenger-utbetaling/.default",
            "SCOPE_DOKUMENT" to "api://prod-gcp.tpts.tiltakspenger-dokument/.default",
            "UTBETALING_URL" to "https://tiltakspenger-utbetaling.intern.nav.no",
            "DOKUMENT_URL" to "http://tiltakspenger-dokument",
        ),
    )

    private val composeProperties = ConfigurationMap(
        mapOf(
            "logback.configurationFile" to "logback.local.xml",
            "DOKUMENT_URL" to System.getenv("DOKUMENT_URL"),
        ),
    )

    private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "dev-gcp" ->
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties overriding defaultProperties

        "prod-gcp" ->
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties overriding defaultProperties

        "compose" ->
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding composeProperties overriding defaultProperties

        else -> {
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties overriding defaultProperties
        }
    }

    data class ClientConfig(
        val baseUrl: String,
    )

    fun utbetalingClientConfig(baseUrl: String = config()[Key("UTBETALING_URL", stringType)]) =
        ClientConfig(baseUrl = baseUrl)

    fun dokumentClientConfig(baseUrl: String = config()[Key("DOKUMENT_URL", stringType)]) =
        ClientConfig(baseUrl = baseUrl)

    fun oauthConfigUtbetaling(
        scope: String = config()[Key("SCOPE_UTBETALING", stringType)],
        clientId: String = config()[Key("AZURE_APP_CLIENT_ID", stringType)],
        clientSecret: String = config()[Key("AZURE_APP_CLIENT_SECRET", stringType)],
        wellknownUrl: String = config()[Key("AZURE_APP_WELL_KNOWN_URL", stringType)],
    ) = AzureTokenProvider.OauthConfig(
        scope = scope,
        clientId = clientId,
        clientSecret = clientSecret,
        wellknownUrl = wellknownUrl,
    )

    fun oauthConfigDokument(
        scope: String = config()[Key("SCOPE_DOKUMENT", stringType)],
        clientId: String = config()[Key("AZURE_APP_CLIENT_ID", stringType)],
        clientSecret: String = config()[Key("AZURE_APP_CLIENT_SECRET", stringType)],
        wellknownUrl: String = config()[Key("AZURE_APP_WELL_KNOWN_URL", stringType)],
    ) = AzureTokenProvider.OauthConfig(
        scope = scope,
        clientId = clientId,
        clientSecret = clientSecret,
        wellknownUrl = wellknownUrl,
    )

    fun logbackConfigurationFile() = config()[Key("logback.configurationFile", stringType)]

    fun httpPort() = config()[Key("application.httpPort", intType)]

    data class DataBaseConf(
        val database: String,
        val host: String,
        val passord: String,
        val port: Int,
        val brukernavn: String,
    )
    fun database() = DataBaseConf(
        database = config()[Key("DB_DATABASE", stringType)],
        host = config()[Key("DB_HOST", stringType)],
        passord = config()[Key("DB_PASSWORD", stringType)],
        brukernavn = config()[Key("DB_USERNAME", stringType)],
        port = config()[Key("DB_PORT", intType)],
    )

    fun electorPath(): String = config()[Key("ELECTOR_PATH", stringType)]
}
