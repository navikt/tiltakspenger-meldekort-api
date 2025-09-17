package no.nav.tiltakspenger.meldekort

import arrow.core.Either
import arrow.core.right
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.AttributeKey
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookup
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupClient
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupFeil
import no.nav.tiltakspenger.libs.jobber.RunCheckFactory
import no.nav.tiltakspenger.libs.periodisering.zoneIdOslo
import no.nav.tiltakspenger.meldekort.Configuration.httpPort
import no.nav.tiltakspenger.meldekort.context.ApplicationContext
import no.nav.tiltakspenger.meldekort.jobber.TaskExecutor
import no.nav.tiltakspenger.meldekort.routes.meldekortApi
import java.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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
) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        log.error(e) { e.message }
    }

    log.info { "starting server" }

    val server = embeddedServer(
        factory = Netty,
        port = port,
        module = {
            meldekortApi(applicationContext = applicationContext)
        },
    )
    server.application.attributes.put(isReadyKey, true)

    val runCheckFactory = if (isNais) {
        RunCheckFactory(
            leaderPodLookup =
            LeaderPodLookupClient(
                electorPath = Configuration.electorPath(),
                logger = KotlinLogging.logger { },
            ),
            attributes = server.application.attributes,
            isReadyKey = isReadyKey,
        )
    } else {
        RunCheckFactory(
            leaderPodLookup =
            object : LeaderPodLookup {
                override fun amITheLeader(localHostName: String): Either<LeaderPodLookupFeil, Boolean> =
                    true.right()
            },
            attributes = server.application.attributes,
            isReadyKey = isReadyKey,
        )
    }

    TaskExecutor.startJob(
        initialDelay = if (isNais) 1.minutes else 1.seconds,
        runCheckFactory = runCheckFactory,
        tasks = listOf<suspend () -> Any?>(
            { applicationContext.sendMeldekortService.sendMeldekort() },
            { applicationContext.journalførMeldekortService.journalførNyeMeldekort() },
            { applicationContext.sendVarslerService.sendVarselForMeldekort() },
            { applicationContext.inaktiverVarslerService.inaktiverVarslerForMottatteMeldekort() },
            { applicationContext.arenaMeldekortStatusService.oppdaterArenaMeldekortStatusForSaker() },
        ).let {
            if (!Configuration.isProd()) {
                it.plus(
                    listOf(
                        { applicationContext.aktiverMicrofrontendJob.aktiverMicrofrontendForBrukere() },
                        { applicationContext.inaktiverMicrofrontendJob.inaktiverMicrofrontendForBrukere() },
                    ),
                )
            } else {
                it
            }
        },
    )

    if (Configuration.isNais()) {
        val consumers = listOf(
            applicationContext.identhendelseConsumer,
        )
        consumers.forEach { it.run() }
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.application.attributes.put(isReadyKey, false)
            server.stop(gracePeriodMillis = 5_000, timeoutMillis = 30_000)
        },
    )
    server.start(wait = true)
}

val isReadyKey = AttributeKey<Boolean>("isReady")

fun Application.isReady() = attributes.getOrNull(isReadyKey) == true
