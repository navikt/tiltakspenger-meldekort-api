package no.nav.tiltakspenger.meldekort.infra

import arrow.core.Either
import arrow.core.right
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookup
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupClient
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupFeil
import no.nav.tiltakspenger.libs.jobber.RunCheckFactory
import no.nav.tiltakspenger.libs.jobber.TaskExecutor
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Readiness
import no.nav.tiltakspenger.libs.ktor.common.oppstart.StoppbarBakgrunnsprosess
import no.nav.tiltakspenger.libs.ktor.common.oppstart.konfigurerLivssyklus
import no.nav.tiltakspenger.libs.ktor.common.oppstart.startMedOpprydding
import no.nav.tiltakspenger.libs.ktor.common.oppstart.stoppbarKafkaConsumer
import no.nav.tiltakspenger.meldekort.infra.routes.CALL_ID_MDC_KEY
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Kobler meldekort-api sine egne bakgrunnsprosesser opp mot den felles livssyklus-orkestreringen i ktor-common ([konfigurerLivssyklus]).
 *
 * [startBakgrunnsprosesser] er injiserbar slik at tester kan verifisere selve oppkoblingen uten å starte ekte jobber/Kafka.
 * I produksjon brukes default-verdien [startStandardBakgrunnsprosesser].
 *
 * Public fordi den brukes både fra [start] og fra tester.
 */
fun Application.konfigurerMeldekortLivssyklus(
    log: KLogger,
    isNais: Boolean,
    applicationContext: ApplicationContext,
    readiness: Readiness,
    shutdownPågår: AtomicBoolean = AtomicBoolean(false),
    startBakgrunnsprosesser: () -> List<StoppbarBakgrunnsprosess> = {
        startStandardBakgrunnsprosesser(
            log = log,
            isNais = isNais,
            applicationContext = applicationContext,
            readiness = readiness,
        )
    },
) {
    konfigurerLivssyklus(
        log = log,
        readiness = readiness,
        shutdownPågår = shutdownPågår,
        startBakgrunnsprosesser = startBakgrunnsprosesser,
    )
}

/**
 * Starter de ekte bakgrunnsprosessene (skedulerte jobber + Kafka-consumer) og returnerer dem som [StoppbarBakgrunnsprosess]-er slik at de kan stoppes rent ved shutdown.
 */
private fun startStandardBakgrunnsprosesser(
    log: KLogger,
    isNais: Boolean,
    applicationContext: ApplicationContext,
    readiness: Readiness,
): List<StoppbarBakgrunnsprosess> = startMedOpprydding(
    log = log,
    startSteg = listOf(
        { startSkedulerteJobber(log = log, isNais = isNais, applicationContext = applicationContext, readiness = readiness) },
        { startIdenthendelseConsumer(log = log, isNais = isNais, applicationContext = applicationContext) },
    ),
)

private fun startSkedulerteJobber(
    log: KLogger,
    isNais: Boolean,
    applicationContext: ApplicationContext,
    readiness: Readiness,
): StoppbarBakgrunnsprosess {
    val initialDelay = if (isNais) 1.minutes else 1.seconds
    log.info { "Starter skedulerte jobber med initialDelay=$initialDelay" }
    val taskExecutor = TaskExecutor.startJob(
        initialDelay = initialDelay,
        runCheckFactory = lagRunCheckFactory(isNais = isNais, readiness = readiness),
        mdcCallIdKey = CALL_ID_MDC_KEY,
        tasks = listOf<suspend (CorrelationId) -> Any?>(
            { applicationContext.sendMeldekortService.sendMeldekort() },
            { applicationContext.journalførMeldekortService.journalførNyeMeldekort() },
            { applicationContext.varselJobber.kjørAlle() },
            { applicationContext.arenaMeldekortStatusService.oppdaterArenaMeldekortStatusForSaker() },
            { applicationContext.aktiverMicrofrontendJob.aktiverMicrofrontendForBrukere() },
            { applicationContext.inaktiverMicrofrontendJob.inaktiverMicrofrontendForBrukere() },
        ),
    )
    log.info { "Skedulerte jobber startet: ${taskExecutor.jobName}" }
    return StoppbarBakgrunnsprosess(navn = "jobb ${taskExecutor.jobName}") { taskExecutor.stop() }
}

private fun startIdenthendelseConsumer(
    log: KLogger,
    isNais: Boolean,
    applicationContext: ApplicationContext,
): StoppbarBakgrunnsprosess? {
    if (!isNais) {
        log.info { "Starter ikke Kafka-consumer identhendelse fordi appen ikke kjører i NAIS" }
        return null
    }
    val consumer = applicationContext.identhendelseConsumer
    log.info { "Starter Kafka-consumer identhendelse" }
    // run() er ikke-blokkerende; den starter konsument-loopen på Dispatchers.IO og returnerer umiddelbart.
    consumer.run()
    log.info { "Kafka-consumer identhendelse startet" }
    return stoppbarKafkaConsumer(log = log, navn = "Kafka-consumer identhendelse") { consumer.stop() }
}

private fun lagRunCheckFactory(isNais: Boolean, readiness: Readiness): RunCheckFactory = if (isNais) {
    RunCheckFactory(
        leaderPodLookup = LeaderPodLookupClient(
            electorPath = Configuration.electorPath(),
            logger = KotlinLogging.logger { },
        ),
        isReady = readiness::erKlar,
    )
} else {
    RunCheckFactory(
        leaderPodLookup = object : LeaderPodLookup {
            override fun amITheLeader(localHostName: String): Either<LeaderPodLookupFeil, Boolean> =
                true.right()
        },
        isReady = readiness::erKlar,
    )
}
