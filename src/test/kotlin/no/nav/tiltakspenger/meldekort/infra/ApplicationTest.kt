package no.nav.tiltakspenger.meldekort.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.ServerReady
import io.ktor.server.testing.testApplication
import io.netty.util.concurrent.DefaultEventExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import no.nav.tiltakspenger.TestApplicationContextMedInMemoryDb
import no.nav.tiltakspenger.meldekort.infra.routes.ktorSetup
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ApplicationTest {
    private val log = KotlinLogging.logger { }

    /**
     * Sjenerøs øvre grense for å vente på at en bakgrunnstråd/-coroutine starter eller fullfører i de concurrency-testene under.
     * Disse er ventegrenser (ikke sleeps), så normal kjøring berører dem aldri.
     */
    private val ventetimeoutMs = 2_000L

    /**
     * Kjører den faktiske livssyklusen ([konfigurerLivssyklus]) sammen med det ekte [ktorSetup]-oppsettet og de ekte bakgrunnsprosessene (skedulert [no.nav.tiltakspenger.libs.jobber.TaskExecutor]).
     * Verifiserer at /isready følger ServerReady -> shutdown slik produksjonskoden faktisk kobler det opp.
     */
    @Test
    fun `faktisk livssyklus markerer appen klar fra ServerReady til shutdown`() = testApplication {
        val context = TestApplicationContextMedInMemoryDb()
        lateinit var app: Application
        application {
            app = this
            ktorSetup(applicationContext = context)
            konfigurerLivssyklus(
                log = log,
                isNais = false,
                applicationContext = context,
            )
        }

        client.get("/isready").apply {
            status shouldBe HttpStatusCode.ServiceUnavailable
            bodyAsText() shouldBe "NOT READY"
        }

        app.monitor.raise(ServerReady, app.environment)

        client.get("/isready").apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe "READY"
        }

        app.monitor.raise(ApplicationStopping, app)

        client.get("/isready").apply {
            status shouldBe HttpStatusCode.ServiceUnavailable
            bodyAsText() shouldBe "NOT READY"
        }
    }

    @Test
    fun `starter bakgrunnsprosesser ved ServerReady og stopper dem nøyaktig en gang ved shutdown`() = testApplication {
        val antallStarter = AtomicInteger(0)
        val stoppet = mutableListOf<String>()
        val context = TestApplicationContextMedInMemoryDb()
        lateinit var app: Application
        application {
            app = this
            ktorSetup(applicationContext = context)
            konfigurerLivssyklus(
                log = log,
                isNais = false,
                applicationContext = context,
                startBakgrunnsprosesser = {
                    antallStarter.incrementAndGet()
                    listOf(
                        StoppbarBakgrunnsprosess(navn = "jobb") { stoppet.add("jobb") },
                        StoppbarBakgrunnsprosess(navn = "kafka") { stoppet.add("kafka") },
                    )
                },
            )
        }

        client.get("/isready").status shouldBe HttpStatusCode.ServiceUnavailable
        antallStarter.get() shouldBe 0

        app.monitor.raise(ServerReady, app.environment)

        antallStarter.get() shouldBe 1
        client.get("/isready").bodyAsText() shouldBe "READY"

        // Duplikat ServerReady skal ikke starte bakgrunnsprosessene på nytt.
        app.monitor.raise(ServerReady, app.environment)
        antallStarter.get() shouldBe 1

        app.monitor.raise(ApplicationStopping, app)

        stoppet shouldBe listOf("jobb", "kafka")
        client.get("/isready").bodyAsText() shouldBe "NOT READY"

        // Duplikat ApplicationStopping skal ikke stoppe dem på nytt.
        app.monitor.raise(ApplicationStopping, app)
        stoppet shouldBe listOf("jobb", "kafka")
    }

    @Test
    fun `starter ikke bakgrunnsprosesser dersom shutdown allerede har startet`() = testApplication {
        val antallStarter = AtomicInteger(0)
        val context = TestApplicationContextMedInMemoryDb()
        lateinit var app: Application
        application {
            app = this
            ktorSetup(applicationContext = context)
            konfigurerLivssyklus(
                log = log,
                isNais = false,
                applicationContext = context,
                startBakgrunnsprosesser = {
                    antallStarter.incrementAndGet()
                    emptyList()
                },
            )
        }

        client.get("/isalive").status shouldBe HttpStatusCode.OK

        app.monitor.raise(ApplicationStopping, app)
        app.monitor.raise(ServerReady, app.environment)

        antallStarter.get() shouldBe 0
        client.get("/isready").bodyAsText() shouldBe "NOT READY"
    }

    @Test
    fun `forsøker å starte bakgrunnsprosesser på nytt ved ny ServerReady hvis første oppstart kaster`() = testApplication {
        val antallStarter = AtomicInteger(0)
        val stoppet = mutableListOf<String>()
        val context = TestApplicationContextMedInMemoryDb()
        lateinit var app: Application
        application {
            app = this
            ktorSetup(applicationContext = context)
            konfigurerLivssyklus(
                log = log,
                isNais = false,
                applicationContext = context,
                startBakgrunnsprosesser = {
                    // Første forsøk kaster, andre forsøk lykkes.
                    if (antallStarter.getAndIncrement() == 0) {
                        error("Kunne ikke starte bakgrunnsprosesser")
                    }
                    listOf(
                        StoppbarBakgrunnsprosess(navn = "jobb") { stoppet.add("jobb") },
                    )
                },
            )
        }

        // Trigger oppstart av app-blokken slik at livssyklusen kobles opp.
        client.get("/isalive").status shouldBe HttpStatusCode.OK

        // Første ServerReady kaster - startet skal IKKE settes, og appen forblir NOT READY.
        shouldThrow<RuntimeException> { app.monitor.raise(ServerReady, app.environment) }
        antallStarter.get() shouldBe 1
        client.get("/isready").bodyAsText() shouldBe "NOT READY"

        // Ny ServerReady skal forsøke på nytt (fordi startet aldri ble satt) og nå lykkes.
        app.monitor.raise(ServerReady, app.environment)
        antallStarter.get() shouldBe 2
        client.get("/isready").bodyAsText() shouldBe "READY"

        // Og prosessene fra det vellykkede forsøket stoppes rent ved shutdown.
        app.monitor.raise(ApplicationStopping, app)
        stoppet shouldBe listOf("jobb")
    }

    @Test
    fun `stopper resterende bakgrunnsprosesser selv om en stop feiler`() = testApplication {
        val stoppet = mutableListOf<String>()
        val context = TestApplicationContextMedInMemoryDb()
        lateinit var app: Application
        application {
            app = this
            ktorSetup(applicationContext = context)
            konfigurerLivssyklus(
                log = log,
                isNais = false,
                applicationContext = context,
                startBakgrunnsprosesser = {
                    listOf(
                        StoppbarBakgrunnsprosess(navn = "feilende jobb") { error("Feil") },
                        StoppbarBakgrunnsprosess(navn = "kafka") { stoppet.add("kafka") },
                    )
                },
            )
        }

        // Trigger oppstart av app-blokken slik at livssyklusen kobles opp.
        client.get("/isalive").status shouldBe HttpStatusCode.OK

        app.monitor.raise(ServerReady, app.environment)
        app.monitor.raise(ApplicationStopping, app)

        stoppet shouldBe listOf("kafka")
    }

    @Test
    fun `shutdown vinner over readiness hvis shutdown starter under oppstart`() = testApplication {
        val antallStarter = AtomicInteger(0)
        val stoppet = mutableListOf<String>()
        val shutdownPågår = AtomicBoolean(false)
        val context = TestApplicationContextMedInMemoryDb()
        lateinit var app: Application
        application {
            app = this
            ktorSetup(applicationContext = context)
            konfigurerLivssyklus(
                log = log,
                isNais = false,
                applicationContext = context,
                shutdownPågår = shutdownPågår,
                startBakgrunnsprosesser = {
                    antallStarter.incrementAndGet()
                    // Simulerer at shutdown-callbacken setter flagget (på en annen tråd) mens vi starter, dvs. etter toppsjekken men før readiness settes.
                    shutdownPågår.set(true)
                    listOf(
                        StoppbarBakgrunnsprosess(navn = "jobb") { stoppet.add("jobb") },
                        StoppbarBakgrunnsprosess(navn = "kafka") { stoppet.add("kafka") },
                    )
                },
            )
        }

        client.get("/isalive").status shouldBe HttpStatusCode.OK

        app.monitor.raise(ServerReady, app.environment)

        // Prosessene ble faktisk startet (toppsjekken slapp gjennom) ...
        antallStarter.get() shouldBe 1
        // ... men siden shutdown startet under oppstart, skal appen rulles tilbake til ikke-klar og prosessene stoppes.
        client.get("/isready").bodyAsText() shouldBe "NOT READY"
        stoppet shouldBe listOf("jobb", "kafka")
    }

    @Test
    fun `signaliserer stopp ved ApplicationStopPreparing og fullfører stopp ved ApplicationStopping`() = testApplication {
        val hendelser = mutableListOf<String>()
        val context = TestApplicationContextMedInMemoryDb()
        lateinit var app: Application
        application {
            app = this
            ktorSetup(applicationContext = context)
            konfigurerLivssyklus(
                log = log,
                isNais = false,
                applicationContext = context,
                startBakgrunnsprosesser = {
                    listOf(
                        StoppbarBakgrunnsprosess(
                            navn = "kafka",
                            stopp = { hendelser.add("stop") },
                            påbegynStopp = { hendelser.add("påbegyntStopp") },
                        ),
                    )
                },
            )
        }

        client.get("/isalive").status shouldBe HttpStatusCode.OK

        app.monitor.raise(ServerReady, app.environment)
        hendelser shouldBe emptyList()

        // ApplicationStopPreparing skal signalisere stopp (men ikke fullføre den ennå).
        app.monitor.raise(ApplicationStopPreparing, app.environment)
        hendelser shouldBe listOf("påbegyntStopp")

        // ApplicationStopping skal fullføre/joine stoppen.
        app.monitor.raise(ApplicationStopping, app)
        hendelser shouldBe listOf("påbegyntStopp", "stop")
    }

    @Test
    fun `stoppbarKafkaConsumer kjører stopp asynkront ved påbegyntStopp og joiner ved stop`() {
        val stoppKalt = AtomicInteger(0)
        val stoppStartet = CountDownLatch(1)
        val slippStopp = CountDownLatch(1)

        val prosess = stoppbarKafkaConsumer(log = log, navn = "test-consumer") {
            stoppKalt.incrementAndGet()
            stoppStartet.countDown()
            // Simulerer at consumeren bruker tid på å fullføre pågående batch.
            slippStopp.await()
        }

        // påbegyntStopp skal sette i gang stoppen asynkront og returnere uten å vente på at den fullføres.
        prosess.påbegynStopp()
        stoppStartet.await(ventetimeoutMs, TimeUnit.MILLISECONDS) shouldBe true
        slippStopp.count shouldBe 1L

        // Slipp løs den simulerte batch-fullføringen og join via stop().
        slippStopp.countDown()
        prosess.stopp()

        // Stoppen kjørte nøyaktig én gang totalt, selv om både påbegyntStopp og stop ble kalt.
        stoppKalt.get() shouldBe 1
    }

    @Test
    fun `stoppbarKafkaConsumer propagerer feil fra stopp-tråden ved stop`() {
        val stoppfeil = RuntimeException("Klarte ikke å committe offsets")

        val prosess = stoppbarKafkaConsumer(log = log, navn = "feilende-consumer") {
            throw stoppfeil
        }

        // Feil fra stopp-tråden skal bli synlig (kastet) når vi joiner i stop(), ikke svelges i bakgrunnstråden.
        val kastet = shouldThrow<RuntimeException> { prosess.stopp() }
        kastet shouldBe stoppfeil
    }

    @Test
    fun `stoppbarKafkaConsumer re-setter interrupt-flagget og rethrower hvis join blir avbrutt`() {
        val slippStopp = CountDownLatch(1)
        val stoppStartet = CountDownLatch(1)

        val prosess = stoppbarKafkaConsumer(log = log, navn = "treg-consumer") {
            stoppStartet.countDown()
            // Blokkerer slik at join() i stop() faktisk venter og kan bli avbrutt.
            slippStopp.await()
        }

        val rethrewInterrupt = AtomicBoolean(false)
        val interruptObservert = AtomicBoolean(false)
        val ventetUtenAvbrudd = AtomicBoolean(false)
        val stopTråd = Thread {
            try {
                prosess.stopp()
            } catch (e: InterruptedException) {
                // stop() skal rethrowe slik at kallende kode (stoppBakgrunnsprosesser) ser at stoppen ikke fullførte.
                rethrewInterrupt.set(true)
                // Interrupt-flagget skal være re-satt selv om vi kaster.
                interruptObservert.set(Thread.currentThread().isInterrupted)
                ventetUtenAvbrudd.set(slippStopp.count == 1L)
            }
        }
        stopTråd.start()

        // Vent til stoppen er i gang (join() blokkerer) og avbryt tråden som joiner.
        stoppStartet.await(ventetimeoutMs, TimeUnit.MILLISECONDS) shouldBe true
        stopTråd.interrupt()
        stopTråd.join(ventetimeoutMs)

        // Vi ga opp ventingen (uten å vente på at den trege stoppen fullførte), kastet videre og bevarte interrupt-flagget.
        rethrewInterrupt.get() shouldBe true
        ventetUtenAvbrudd.get() shouldBe true
        interruptObservert.get() shouldBe true

        slippStopp.countDown()
    }

    @Test
    fun `stoppbarKafkaConsumer river ned scopet etter at stoppen er fullført`() {
        val stoppScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        val prosess = stoppbarKafkaConsumer(
            log = log,
            navn = "ryddig-consumer",
            stoppScope = stoppScope,
        ) {
            // Stoppen fullfører umiddelbart.
        }

        stoppScope.isActive shouldBe true
        prosess.stopp()

        // Etter at stoppen er fullført skal det dedikerte scopet være kansellert, ikke etterlatt levende.
        stoppScope.isActive shouldBe false
    }

    @Test
    fun `stoppbarKafkaConsumer kan stoppes flere ganger uten å feile`() {
        val stoppKalt = AtomicInteger(0)
        val stoppScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        val prosess = stoppbarKafkaConsumer(
            log = log,
            navn = "consumer",
            stoppScope = stoppScope,
        ) {
            stoppKalt.incrementAndGet()
        }

        // Første stop fullfører og river ned scopet.
        prosess.stopp()
        // Et nytt påbegyntStopp etter at scopet er kansellert skal ikke opprette en ny async på det kansellerte scopet
        // (det ville kastet), fordi stoppJobb allerede er satt og vi returnerer tidlig.
        prosess.påbegynStopp()
        // En ny stop() skal heller ikke feile (await på en ferdig jobb + idempotent cancel).
        prosess.stopp()

        // Stoppen kjørte fortsatt nøyaktig én gang totalt.
        stoppKalt.get() shouldBe 1
        stoppScope.isActive shouldBe false
    }

    @Test
    fun `påbegyntStopp oppretter ikke ubrukte child-jobber når stoppen allerede er igangsatt`() {
        val stoppScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val stoppStartet = CountDownLatch(1)
        val slippStopp = CountDownLatch(1)

        val prosess = stoppbarKafkaConsumer(
            log = log,
            navn = "consumer",
            stoppScope = stoppScope,
        ) {
            stoppStartet.countDown()
            // Blokker stoppen slik at den første jobben fortsatt kjører mens vi kaller påbegyntStopp på nytt.
            slippStopp.await()
        }

        // Start stoppen (skal gi nøyaktig én kjørende child-jobb i scopet).
        prosess.påbegynStopp()
        stoppStartet.await(ventetimeoutMs, TimeUnit.MILLISECONDS) shouldBe true
        stoppScope.coroutineContext.job.children.count() shouldBe 1

        // Kall påbegyntStopp mange ganger mens stoppen fortsatt kjører. Hvert kall skal returnere tidlig (og et evt.
        // CAS-tap kanselleres umiddelbart), så det skal ikke samle seg opp ubrukte, ikke-startede LAZY-jobber i scopet.
        repeat(20) { prosess.påbegynStopp() }
        stoppScope.coroutineContext.job.children.count() shouldBe 1

        // Rydd opp.
        slippStopp.countDown()
        prosess.stopp()
    }

    @Test
    fun `gjenkjenner kun Netty event executor terminated startup-feil`() {
        RejectedExecutionException("event executor terminated").erNettyEventExecutorTerminert() shouldBe true
        RejectedExecutionException("annen feil").erNettyEventExecutorTerminert() shouldBe false
        RuntimeException("event executor terminated").erNettyEventExecutorTerminert() shouldBe false
    }

    @Test
    fun `Netty kaster fortsatt event executor terminated - låser strengen mot ekte Netty-oppførsel`() {
        // erNettyEventExecutorTerminert() matcher på meldingstekst fordi Netty ikke gir en mer spesifikk exception-type.
        // Her trigger vi den faktiske feilen ved å sende en oppgave til en terminert executor (samme kodevei i Netty som under shutdown-race-en).
        // Endrer en Ktor/Netty-oppgradering meldingen, feiler denne testen i stedet for at vi stille slutter å gjenkjenne racen i produksjon.
        val executor = DefaultEventExecutor()
        // await() returnerer true når nedstengningen faktisk fullførte innen timeouten. Sjekk den, ellers kan executor
        // fortsatt være under nedstenging når vi kaller execute under, og da kastes ikke nødvendigvis forventet exception (flaky).
        executor.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS).await(ventetimeoutMs) shouldBe true

        val kastet = shouldThrow<RejectedExecutionException> {
            executor.execute { }
        }

        kastet.message shouldBe "event executor terminated"
        kastet.erNettyEventExecutorTerminert() shouldBe true
    }

    @Test
    fun `startMedOpprydding stopper allerede startede prosesser hvis et senere steg feiler`() {
        val stoppet = mutableListOf<String>()
        val oppstartsfeil = RuntimeException("Kunne ikke starte Kafka-consumer")

        val kastet = shouldThrow<RuntimeException> {
            startMedOpprydding(
                log = log,
                startSteg = listOf(
                    { StoppbarBakgrunnsprosess(navn = "jobb") { stoppet.add("jobb") } },
                    { throw oppstartsfeil },
                ),
            )
        }

        // Selve feilen skal kastes videre slik at oppstart ikke fortsetter som om alt gikk bra ...
        kastet shouldBe oppstartsfeil
        // ... men den allerede startede prosessen må stoppes, ellers lever den videre uten en vei til å stoppes.
        stoppet shouldBe listOf("jobb")
    }

    @Test
    fun `startMedOpprydding returnerer startede prosesser og hopper over steg som gir null`() {
        val stoppet = mutableListOf<String>()

        val prosesser = startMedOpprydding(
            log = log,
            startSteg = listOf(
                { StoppbarBakgrunnsprosess(navn = "jobb") { stoppet.add("jobb") } },
                { null },
                { StoppbarBakgrunnsprosess(navn = "kafka") { stoppet.add("kafka") } },
            ),
        )

        prosesser.map { it.navn } shouldBe listOf("jobb", "kafka")
        stoppet shouldBe emptyList()
    }
}
