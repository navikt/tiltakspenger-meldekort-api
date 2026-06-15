package no.nav.tiltakspenger.meldekort.infra

import arrow.core.Either
import arrow.core.right
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.ServerReady
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookup
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupClient
import no.nav.tiltakspenger.libs.jobber.LeaderPodLookupFeil
import no.nav.tiltakspenger.libs.jobber.RunCheckFactory
import no.nav.tiltakspenger.libs.jobber.TaskExecutor
import no.nav.tiltakspenger.meldekort.infra.routes.CALL_ID_MDC_KEY
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val isReadyKey = AttributeKey<Boolean>("isReady")

/** Public fordi den brukes av healthRoutes (`/isready`) i en annen pakke. */
fun Application.isReady() = attributes.getOrNull(isReadyKey) == true

/**
 * Kobler opp livssyklusen til appen:
 *  - markerer appen som ikke-klar med en gang og ved shutdown ([markerSomIkkeKlarVedShutdown]),
 *  - starter bakgrunnsprosesser (skedulerte jobber + Kafka) når Netty er ferdig å binde ([ServerReady]),
 *  - stopper dem rent ved shutdown ([ApplicationStopping]).
 *
 * [startBakgrunnsprosesser] er injiserbar slik at tester kan verifisere selve orkestreringen (rekkefølge, idempotens, shutdown-race) uten å starte ekte jobber/Kafka.
 * I produksjon brukes [startStandardBakgrunnsprosesser].
 *
 * Public fordi den brukes både fra [start] og fra tester.
 */
fun Application.konfigurerLivssyklus(
    log: KLogger,
    isNais: Boolean,
    applicationContext: ApplicationContext,
    shutdownPågår: AtomicBoolean = AtomicBoolean(false),
    startBakgrunnsprosesser: () -> List<StoppbarBakgrunnsprosess> = {
        startStandardBakgrunnsprosesser(
            log = log,
            isNais = isNais,
            applicationContext = applicationContext,
        )
    },
) {
    markerSomIkkeKlarVedShutdown(log = log, shutdownPågår = shutdownPågår)

    val livssyklus = Bakgrunnsprosesslivssyklus(
        log = log,
        shutdownPågår = shutdownPågår,
        startBakgrunnsprosesser = startBakgrunnsprosesser,
    )

    monitor.subscribe(ServerReady) {
        // ServerReady fyres av Netty etter at bind() er ferdig.
        // Vi venter på denne tilstanden før appen markeres som klar og før bakgrunnsjobber/Kafka starter, slik at vi ikke prosesserer arbeid mens HTTP-serveren fortsatt er i startup.
        // Se Ktor lifecycle-events: https://ktor.io/docs/server-events.html
        // Se også Ktor NettyApplicationEngine.start: https://github.com/ktorio/ktor/blob/3.4.3/ktor-server/ktor-server-netty/jvm/src/io/ktor/server/netty/NettyApplicationEngine.kt
        livssyklus.startVedServerReady(this)
    }

    monitor.subscribe(ApplicationStopPreparing) {
        // ApplicationStopPreparing er det første shutdown-signalet (før HTTP-grace-perioden).
        // Vi signaliserer stopp til bakgrunnsprosessene her slik at f.eks. Kafka slutter å plukke nye records med en gang, mens den blokkerende ventingen joines inn ved ApplicationStopping.
        livssyklus.påbegyntStoppVedShutdown()
    }

    monitor.subscribe(ApplicationStopping) {
        livssyklus.stopp()
    }
}

private fun Application.markerSomIkkeKlarVedShutdown(log: KLogger, shutdownPågår: AtomicBoolean) {
    attributes.put(isReadyKey, false)

    monitor.subscribe(ApplicationStopPreparing) {
        log.info { "ApplicationStopPreparing mottatt - markerer appen som ikke klar" }
        shutdownPågår.set(true)
        attributes.put(isReadyKey, false)
    }

    monitor.subscribe(ApplicationStopping) {
        log.info { "ApplicationStopping mottatt - markerer appen som ikke klar" }
        // ApplicationStopPreparing er ikke garantert å ha blitt fyrt (f.eks. ved enkelte brå shutdowns), så vi setter flagget her også.
        // Idempotent: gjentatt set(true) er ufarlig, og sikrer at appen aldri står igjen som klar.
        shutdownPågår.set(true)
        attributes.put(isReadyKey, false)
    }
}

/**
 * Starter de ekte bakgrunnsprosessene (skedulerte jobber + Kafka-consumer) og returnerer dem som [StoppbarBakgrunnsprosess]-er slik at de kan stoppes rent ved shutdown.
 */
private fun Application.startStandardBakgrunnsprosesser(
    log: KLogger,
    isNais: Boolean,
    applicationContext: ApplicationContext,
): List<StoppbarBakgrunnsprosess> = startMedOpprydding(
    log = log,
    startSteg = listOf(
        { startSkedulerteJobber(log = log, isNais = isNais, applicationContext = applicationContext) },
        { startIdenthendelseConsumer(log = log, isNais = isNais, applicationContext = applicationContext) },
    ),
)

/**
 * Starter stegene i rekkefølge og samler [StoppbarBakgrunnsprosess]-ene (steg som returnerer null hoppes over).
 * Hvis et steg kaster, stoppes de allerede startede prosessene før unntaket kastes videre, slik at vi ikke etterlater delvis startede prosesser uten en vei til å stoppe dem.
 * Public pga. test.
 */
fun startMedOpprydding(
    log: KLogger,
    startSteg: List<() -> StoppbarBakgrunnsprosess?>,
): List<StoppbarBakgrunnsprosess> {
    val startede = mutableListOf<StoppbarBakgrunnsprosess>()
    // Either.catch re-kaster fatale throwables (via nonFatalOrThrow), så vi rydder kun opp ved ikke-fatale feil.
    Either.catch {
        startSteg.forEach { steg -> steg()?.let { startede.add(it) } }
    }.onLeft { feil ->
        log.error(feil) { "Feil under oppstart av bakgrunnsprosesser - stopper ${startede.size} allerede startet prosess(er)" }
        stoppBakgrunnsprosesser(log = log, bakgrunnsprosesser = startede)
        throw feil
    }
    return startede
}

private fun Application.startSkedulerteJobber(
    log: KLogger,
    isNais: Boolean,
    applicationContext: ApplicationContext,
): StoppbarBakgrunnsprosess {
    val initialDelay = if (isNais) 1.minutes else 1.seconds
    log.info { "Starter skedulerte jobber med initialDelay=$initialDelay" }
    val taskExecutor = TaskExecutor.startJob(
        initialDelay = initialDelay,
        runCheckFactory = lagRunCheckFactory(isNais = isNais),
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

/**
 * Pakker en blokkerende consumer-stopp ([stopp], typisk ManagedKafkaConsumer.stop) inn i en to-fase [StoppbarBakgrunnsprosess].
 * consumer.stop() slutter å polle umiddelbart, men venter så på at pågående batch behandles og committes (opptil shutdownTimeout).
 * Vi kjører [stopp] i en egen coroutine på [Dispatchers.IO] slik at consumeren slutter å plukke nye records i det shutdown starter (`påbegyntStopp` ved ApplicationStopPreparing).
 * Den blokkerende ventingen joines inn ved den endelige stoppen (`stop` ved ApplicationStopping), så nedstengningen overlapper HTTP-grace-perioden i stedet for å komme i tillegg.
 * Det dedikerte [stoppScope] rives ned igjen i `stop` (injiserbart for test).
 * Public pga. test.
 *
 * @param stoppScope Dedikert scope stopp-coroutinen kjøres i; injiserbart for test. Default er et nytt scope på [Dispatchers.IO] med [SupervisorJob], og det rives ned (`cancel`) i `stop`.
 * @param stopp Den blokkerende stoppen som skal kjøres. Må være selvstendig blokkerende og kan ikke avhenge av Ktor-/applikasjonstråder for å fullføre, siden den joines via `runBlocking` fra Ktor sin blokkerende shutdown-callback (ellers risikerer vi dødlås under shutdown).
 */
fun stoppbarKafkaConsumer(
    log: KLogger,
    navn: String,
    // SupervisorJob slik at en feilende stopp ikke kansellerer scopet, men i stedet fanges av Deferred og kastes ved await().
    stoppScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    stopp: () -> Unit,
): StoppbarBakgrunnsprosess {
    val stoppJobb = AtomicReference<Deferred<Unit>?>(null)

    fun påbegynnStopp() {
        // Vanlig tilfelle: stoppen er allerede igangsatt (påbegyntStopp + stop), så vi slipper å opprette en ny jobb.
        if (stoppJobb.get() != null) return
        // start = LAZY slik at kun den jobben som faktisk vinner compareAndSet blir startet (idempotent stopp).
        val nyJobb = stoppScope.async(start = CoroutineStart.LAZY) { stopp() }
        if (stoppJobb.compareAndSet(null, nyJobb)) {
            log.info { "Signaliserer stopp til $navn" }
            nyJobb.start()
        } else {
            // Tapte kappløpet (en annen tråd vant CAS). Kanseller den ubrukte LAZY-jobben med en gang
            // så den ikke henger igjen som et ikke-startet child i scopet til scopet rives ned.
            nyJobb.cancel()
        }
    }

    return StoppbarBakgrunnsprosess(
        navn = navn,
        stopp = {
            // Sikrer at stoppen er igangsatt (selv om ApplicationStopPreparing ikke ble fyrt) og venter på at den fullføres.
            påbegynnStopp()
            val jobb = stoppJobb.get()
            try {
                // stop() kalles fra Ktor sin blokkerende shutdown-callback, så vi joiner via runBlocking.
                // await() kaster videre en evt. feil fra stopp-coroutinen, slik at shutdown-problemer blir synlige (og håndteres/logges av stoppBakgrunnsprosesser).
                runBlocking { jobb?.await() }
            } catch (e: InterruptedException) {
                // runBlocking kaster InterruptedException hvis shutdown-tråden blir avbrutt mens vi venter.
                // Bevar interrupt-flagget slik at kallende kode ser avbruddet.
                // Rethrow slik at stoppBakgrunnsprosesser logger en ekte feil (onLeft) i stedet for falsk suksess ("Stoppet") - stoppen fullførte ikke, og kontrakten på stop ("Fullfører/venter") er brutt.
                Thread.currentThread().interrupt()
                throw e
            } finally {
                // Riv ned det dedikerte scopet når vi er ferdige med å vente, slik at vi ikke etterlater et levende scope etter shutdown.
                // Ble vi avbrutt, kanselleres en evt. fortsatt kjørende stopp-coroutine her (den blokkerende stoppen rekker uansett å fullføre siden den ikke er samarbeidende om kansellering).
                stoppScope.cancel()
            }
        },
        påbegynStopp = { påbegynnStopp() },
    )
}

private fun Application.lagRunCheckFactory(isNais: Boolean): RunCheckFactory = if (isNais) {
    RunCheckFactory(
        leaderPodLookup = LeaderPodLookupClient(
            electorPath = Configuration.electorPath(),
            logger = KotlinLogging.logger { },
        ),
        attributes = attributes,
        isReadyKey = isReadyKey,
    )
} else {
    RunCheckFactory(
        leaderPodLookup = object : LeaderPodLookup {
            override fun amITheLeader(localHostName: String): Either<LeaderPodLookupFeil, Boolean> =
                true.right()
        },
        attributes = attributes,
        isReadyKey = isReadyKey,
    )
}

/**
 * Holder på livssyklusen til bakgrunnsprosessene og sørger for at start og stopp er trygge på tvers av tråder.
 *
 * Både [startVedServerReady] (kjøres på ServerReady-tråden) og [stopp] (kjøres på shutdown-tråden) tar samme lås.
 * Det gir disse garantiene:
 *  1. Bakgrunnsprosessene startes nøyaktig én gang, selv om [ServerReady] skulle fyres flere ganger.
 *  2. De stoppes nøyaktig én gang (felles `stoppet`-flagg), selv om både "shutdown kom mens vi startet"-grenen og [ApplicationStopping] prøver å stoppe.
 *  3. Hvis shutdown kommer mens vi starter, venter stoppen på at starten blir ferdig (samme lås), slik at vi aldri "mister" en nettopp startet prosess og lar den leve videre.
 *  4. Shutdown vinner alltid over readiness: `shutdownPågår` settes av shutdown-callbacken uten låsen, så vi re-sjekker flagget etter at readiness settes og ruller tilbake til ikke-klar + stopp dersom shutdown startet i mellomtiden.
 *  5. Stopp skjer i to faser: ved [ApplicationStopPreparing] signaliseres stopp ([påbegyntStoppVedShutdown]) slik at f.eks. Kafka slutter å plukke nye records med en gang, mens den blokkerende ventingen joines inn ved [ApplicationStopping] ([stopp]).
 */
private class Bakgrunnsprosesslivssyklus(
    private val log: KLogger,
    private val shutdownPågår: AtomicBoolean,
    private val startBakgrunnsprosesser: () -> List<StoppbarBakgrunnsprosess>,
) {
    private val lås = Any()
    private var startet = false
    private var påbegyntStopp = false
    private var stoppet = false
    private var bakgrunnsprosesser: List<StoppbarBakgrunnsprosess> = emptyList()

    fun startVedServerReady(application: Application) {
        synchronized(lås) {
            if (startet) {
                log.info { "ServerReady mottatt flere ganger - bakgrunnsprosesser er allerede startet" }
                return
            }

            if (stoppet || shutdownPågår.get()) {
                log.info { "ServerReady mottatt etter at shutdown har startet - starter ikke bakgrunnsprosesser" }
                return
            }

            log.info { "ServerReady mottatt - starter bakgrunnsprosesser" }
            bakgrunnsprosesser = startBakgrunnsprosesser()
            // Marker først som startet etter vellykket oppstart. Kaster startBakgrunnsprosesser(), forblir startet = false slik
            // at en ev. ny ServerReady kan forsøke på nytt (i stedet for å bli permanent NOT READY uten å prøve igjen).
            startet = true

            application.attributes.put(isReadyKey, true)
            log.info { "Bakgrunnsprosesser er startet - applikasjonen er klar" }

            // shutdownPågår kan settes på en annen tråd mellom toppsjekken og at vi setter isReady=true.
            // Shutdown-callbacken tar ikke samme lås, så uten denne re-sjekken kunne appen endt som READY etter at shutdown startet.
            // Shutdown skal alltid vinne, så vi ruller tilbake readiness og stopper prosessene dersom shutdown startet i mellomtiden.
            if (shutdownPågår.get()) {
                log.info { "Shutdown startet under oppstart - ruller tilbake readiness og stopper bakgrunnsprosesser" }
                application.attributes.put(isReadyKey, false)
                stoppInternt()
            }
        }
    }

    fun påbegyntStoppVedShutdown() {
        synchronized(lås) {
            if (påbegyntStopp) {
                return
            }
            påbegyntStopp = true

            if (bakgrunnsprosesser.isEmpty()) {
                return
            }
            log.info { "ApplicationStopPreparing mottatt - signaliserer stopp til ${bakgrunnsprosesser.size} bakgrunnsprosess(er)" }
            bakgrunnsprosesser.forEach { bakgrunnsprosess ->
                Either.catch { bakgrunnsprosess.påbegynStopp() }
                    .onLeft { log.error(it) { "Kunne ikke påbegynne stopp av ${bakgrunnsprosess.navn}" } }
            }
        }
    }

    fun stopp() {
        synchronized(lås) {
            log.info { "ApplicationStopping mottatt - stopper bakgrunnsprosesser" }
            stoppInternt()
        }
    }

    private fun stoppInternt() {
        if (stoppet) {
            log.info { "Bakgrunnsprosesser er allerede stoppet - ignorerer" }
            return
        }
        stoppet = true
        stoppBakgrunnsprosesser(log = log, bakgrunnsprosesser = bakgrunnsprosesser)
    }
}

private fun stoppBakgrunnsprosesser(
    log: KLogger,
    bakgrunnsprosesser: List<StoppbarBakgrunnsprosess>,
) {
    if (bakgrunnsprosesser.isEmpty()) {
        log.info { "Ingen bakgrunnsprosesser å stoppe" }
        return
    }

    log.info { "Stopper ${bakgrunnsprosesser.size} bakgrunnsprosess(er)" }
    bakgrunnsprosesser.forEach { bakgrunnsprosess ->
        log.info { "Stopper ${bakgrunnsprosess.navn}" }
        Either.catch { bakgrunnsprosess.stopp() }
            .onLeft { log.error(it) { "Kunne ikke stoppe ${bakgrunnsprosess.navn} ved shutdown" } }
            .onRight { log.info { "Stoppet ${bakgrunnsprosess.navn}" } }
    }
    log.info { "Ferdig med å stoppe bakgrunnsprosesser" }
}

/** Public fordi den injiseres i [konfigurerLivssyklus] og konstrueres i tester. */
data class StoppbarBakgrunnsprosess(
    val navn: String,
    /**
     * Signaliserer stopp tidlig uten å blokkere lenge.
     * Kalles ved ApplicationStopPreparing.
     * Default: ingenting.
     */
    val påbegynStopp: () -> Unit = {},
    /**
     * Fullfører/venter på at stopp er ferdig.
     * Kalles ved ApplicationStopping.
     * Ligger sist så den kan brukes som trailing-lambda.
     */
    val stopp: () -> Unit,
)
