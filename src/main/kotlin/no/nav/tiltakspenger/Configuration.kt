package no.nav.tiltakspenger

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding

object Configuration {

    private val defaultProperties = ConfigurationMap(
        mapOf(
            "application.httpPort" to 8080.toString(),
            )
    )

    private fun config() =
        when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
            "dev-gcp" ->
                systemProperties() overriding defaultProperties

            "prod-gcp" ->
                systemProperties() overriding defaultProperties

            else -> {
                systemProperties() overriding defaultProperties
            }
        }

    fun httpPort() = config()[Key("application.httpPort", intType)]

}