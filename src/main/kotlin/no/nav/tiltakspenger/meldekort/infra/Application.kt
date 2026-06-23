package no.nav.tiltakspenger.meldekort.infra

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.meldekort.infra.Configuration.httpPort
import no.nav.tiltakspenger.meldekort.infra.routes.ktorSetup
import java.time.Clock
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}

    start(log = log)
}

fun start(
    log: KLogger,
    port: Int = httpPort(),
    isNais: Boolean = Configuration.isNais(),
    applicationContext: ApplicationContext = ApplicationContext(
        clock = Clock.system(zoneIdOslo),
    ),
    additionalRoutes: (io.ktor.server.routing.Routing.() -> Unit)? = null,
) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e.message }
    }

    log.info { "starting server" }

    val shutdownPågår = AtomicBoolean(false)

    val server = embeddedServer(
        factory = Netty,
        configure = {
            connector {
                this.port = port
            }
            shutdownGracePeriod = 5_000
            shutdownTimeout = 30_000
        },
        module = {
            konfigurerLivssyklus(
                log = log,
                isNais = isNais,
                applicationContext = applicationContext,
                shutdownPågår = shutdownPågår,
            )
            ktorSetup(applicationContext = applicationContext, additionalRoutes = additionalRoutes)
        },
    )

    try {
        server.start(wait = true)
    } catch (e: RejectedExecutionException) {
        // Ved redeploy kan SIGTERM treffe akkurat mens Netty binder server-socketen.
        // Ktor registrerer sin egen shutdown hook i EmbeddedServer.start(), og dersom den rekker å terminere Netty EventLoopGroup før bind er ferdig, kan Netty kaste RejectedExecutionException("event executor terminated") fra start().
        // Det er en forventet shutdown-race, ikke en reell oppstartsfeil.
        // Kilder:
        // - Ktor lifecycle-events: https://ktor.io/docs/server-events.html
        // - Ktor shutdown: https://ktor.io/docs/server-shutdown.html
        // - Ktor EmbeddedServer.start/stop: https://github.com/ktorio/ktor/blob/3.4.3/ktor-server/ktor-server-core/jvm/src/io/ktor/server/engine/EmbeddedServerJvm.kt
        // - Ktor NettyApplicationEngine.start/stop: https://github.com/ktorio/ktor/blob/3.4.3/ktor-server/ktor-server-netty/jvm/src/io/ktor/server/netty/NettyApplicationEngine.kt
        // - Netty EventLoopGroup shutdownGracefully: https://netty.io/4.1/api/io/netty/channel/EventLoopGroup.html
        if (shutdownPågår.get() && e.erNettyEventExecutorTerminert()) {
            log.info(e) { "Ignorerer Netty startup-feil fordi shutdown allerede pågår" }
        } else {
            throw e
        }
    }
}

/**
 * Den eksakte (substring av) feilmeldingen Netty bruker når EventLoopGroup allerede er terminert.
 * Vi matcher på tekst fordi Netty ikke gir en mer spesifikk exception-type her, så denne må verifiseres ved oppgradering av Ktor/Netty (det finnes en test som låser eksakt streng: erNettyEventExecutorTerminert).
 */
private const val NETTY_EVENT_EXECUTOR_TERMINATED = "event executor terminated"

/**
 * Sjekker om feilen er Netty sin "event executor terminated"-feil som kan oppstå når SIGTERM treffer under oppstart.
 * Matcher på meldingstekst, så den må verifiseres ved oppgradering av Ktor/Netty.
 */
fun Throwable.erNettyEventExecutorTerminert(): Boolean =
    this is RejectedExecutionException && message?.contains(NETTY_EVENT_EXECUTOR_TERMINATED) == true
