package no.nav.tiltakspenger.meldekort.infra

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Bakgrunnsprosessoppsett
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Jobboppsett
import no.nav.tiltakspenger.libs.ktor.common.oppstart.startApp
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.meldekort.infra.routes.CALL_ID_MDC_KEY
import no.nav.tiltakspenger.meldekort.infra.routes.ktorSetup
import java.time.Clock

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile)

    val log = KotlinLogging.logger {}

    start(log = log)
}

/**
 * Komposisjonsroten.
 * Her konstrueres registeret alle appens målinger føres i: Ktor-metrikkene, jobbmålingene og meldingsleser-målingene.
 * Det er det samme registeret `/metrics` skraper, så sender vi inn et annet register ett av stedene, forsvinner seriene stille.
 * Registeret er appens eget og bindes ikke til Prometheus sitt globale register, siden ingenting i dette repoet registrerer målinger der.
 * Tester lager sitt eget register, fordi et prosessnavn bare kan registreres én gang per register.
 */
fun start(
    log: KLogger,
    port: Int = Configuration.httpPort,
    host: String = "0.0.0.0",
    isNais: Boolean = Configuration.isNais(),
    applicationContext: ApplicationContext = ApplicationContext(
        clock = Clock.system(zoneIdOslo),
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    ),
    additionalRoutes: (io.ktor.server.routing.Routing.() -> Unit)? = null,
) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e.message }
    }

    startApp(
        log = log,
        port = port,
        host = host,
        isNais = isNais,
        oppsett = Bakgrunnsprosessoppsett(
            jobber = Jobboppsett(
                mdcCallIdKey = CALL_ID_MDC_KEY,
                electorPath = { Configuration.electorPath },
                clock = applicationContext.clock,
                meterRegistry = applicationContext.meterRegistry,
                tasks = jobber(applicationContext),
            ),
            kafkaConsumers = kafkaConsumers(isNais = isNais, applicationContext = applicationContext),
        ),
    ) { readiness ->
        ktorSetup(applicationContext = applicationContext, readiness = readiness, additionalRoutes = additionalRoutes)
    }
}
