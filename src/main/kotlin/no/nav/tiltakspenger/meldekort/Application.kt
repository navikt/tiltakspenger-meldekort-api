package no.nav.tiltakspenger.meldekort

import arrow.core.Either
import arrow.core.right
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KLogger
import mu.KotlinLogging
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookup
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupClient
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupFeil
import no.nav.tiltakspenger.libs.jobber.RunCheckFactory
import no.nav.tiltakspenger.libs.logging.sikkerlogg
import no.nav.tiltakspenger.meldekort.Configuration.httpPort
import no.nav.tiltakspenger.meldekort.context.ApplicationContext
import no.nav.tiltakspenger.meldekort.jobber.TaskExecutor
import no.nav.tiltakspenger.meldekort.routes.meldekortApi
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun main() {
    System.setProperty("logback.configurationFile", Configuration.logbackConfigurationFile())

    val log = KotlinLogging.logger {}

    start(log = log)
}

internal fun start(
    log: KLogger,
    port: Int = httpPort(),
    isNais: Boolean = Configuration.isNais(),
    applicationContext: ApplicationContext = ApplicationContext(),
) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error { "Uncaught exception logget i securelog" }
        sikkerlogg.error(e) { e.message }
    }

    log.info { "starting server" }

    val runCheckFactory = if (isNais) {
        RunCheckFactory(
            leaderPodLookup =
            LeaderPodLookupClient(
                electorPath = Configuration.electorPath(),
                logger = KotlinLogging.logger { },
            ),
        )
    } else {
        RunCheckFactory(
            leaderPodLookup =
            object : LeaderPodLookup {
                override fun amITheLeader(localHostName: String): Either<LeaderPodLookupFeil, Boolean> =
                    true.right()
            },
        )
    }

    TaskExecutor.startJob(
        initialDelay = if (isNais) 1.minutes else 1.seconds,
        runCheckFactory = runCheckFactory,
        tasks =
        listOf { correlationId ->
            applicationContext.sendMeldekortService.sendMeldekort(correlationId)
        },
    )

    embeddedServer(
        factory = Netty,
        port = port,
        module = {
            meldekortApi(applicationContext = applicationContext)
        },
    ).start(wait = true)
}
