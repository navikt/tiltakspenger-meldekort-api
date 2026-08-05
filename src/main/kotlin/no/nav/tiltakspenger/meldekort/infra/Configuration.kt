package no.nav.tiltakspenger.meldekort.infra

private const val APPLICATION_NAME = "tiltakspenger-meldekort-api"

const val KAFKA_CONSUMER_GROUP_ID = "$APPLICATION_NAME-consumer"

private fun hentConfigForMiljø(): EnvironmentConfig {
    return when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "prod-gcp" -> ProdConfig
        "dev-gcp" -> DevConfig
        else -> LocalConfig
    }
}

object Configuration : EnvironmentConfig by hentConfigForMiljø() {
    fun isNais(): Boolean = profile != Profile.LOCAL

    fun isProd(): Boolean = profile == Profile.PROD

    fun isDev(): Boolean = profile == Profile.DEV
}
