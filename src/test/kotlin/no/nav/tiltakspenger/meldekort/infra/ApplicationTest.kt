package no.nav.tiltakspenger.meldekort.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.ServerReady
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.TestApplicationContextMedInMemoryDb
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Readiness
import no.nav.tiltakspenger.libs.ktor.common.oppstart.konfigurerOppstart
import no.nav.tiltakspenger.meldekort.infra.routes.ktorSetup
import org.junit.jupiter.api.Test

/**
 * Den generiske oppstarts-/livssyklus-orkestreringen (idempotent start/stopp, shutdown-race, stoppbarKafkaConsumer, startMedOpprydding, Netty-streng-lås) testes i ktor-common (`OppstartTest`).
 * Her verifiserer vi kun at meldekort-api faktisk wirer felles-mønsteret riktig sammen med sitt eget [ktorSetup] og sine ekte bakgrunnsprosesser.
 */
class ApplicationTest {
    private val log = KotlinLogging.logger { }

    /**
     * Kjører den faktiske meldekort-oppstarten ([konfigurerOppstart] med [jobber] + [kafkaConsumers]) sammen med det ekte [ktorSetup]-oppsettet og de ekte bakgrunnsprosessene (skedulert [no.nav.tiltakspenger.libs.jobber.TaskExecutor]).
     * Verifiserer at /isready følger ServerReady -> shutdown slik produksjonskoden faktisk kobler det opp, inkludert at samme [Readiness] deles av healthRoutes og livssyklusen.
     */
    @Test
    fun `faktisk livssyklus markerer appen klar fra ServerReady til shutdown`() = testApplication {
        val context = TestApplicationContextMedInMemoryDb()
        val readiness = Readiness()
        lateinit var app: Application
        application {
            app = this
            ktorSetup(applicationContext = context, readiness = readiness)
            konfigurerOppstart(
                log = log,
                isNais = false,
                readiness = readiness,
                // isNais = false gir lokal leader election, så electorPath leses aldri, og tom consumer-liste.
                oppsett = bakgrunnsprosessoppsett(applicationContext = context, isNais = false),
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

    /**
     * Verifiserer at registeret jobbene og consumeren skriver målingene sine til, er det samme registeret `/metrics` skraper.
     * Det er hele poenget med at [ApplicationContext] eier registeret: sender vi inn et annet register i `Jobboppsett` eller i consumeren, forsvinner seriene stille, og varselreglene «Jobb har stoppet» og «Meldingsleser har stoppet» får aldri data.
     * Oppsettet er det samme som [start] bruker, siden begge bygger det med [bakgrunnsprosessoppsett], så en feil i parameteroverføringen i komposisjonsroten fanges også her.
     *
     * Consumeren konstrueres, men startes ikke.
     * Meldingsleser-målingene registreres i konstruktøren til [no.nav.tiltakspenger.libs.kafka.infra.ManagedKafkaConsumer], mens `run()` ville krevd en ekte Kafka-broker.
     * Jobbmålingene registreres når skedulereren starter, altså ved [ServerReady].
     */
    @Test
    fun `jobbene og consumeren fører målingene sine i registeret metrics skraper`() = testApplication {
        val context = TestApplicationContextMedInMemoryDb()
        val readiness = Readiness()
        lateinit var app: Application
        application {
            app = this
            ktorSetup(applicationContext = context, readiness = readiness)
            konfigurerOppstart(
                log = log,
                isNais = false,
                readiness = readiness,
                // isNais = false gir lokal leader election, så electorPath leses aldri, og tom consumer-liste.
                // Consumerne startes ikke her; det ville krevd en ekte broker.
                oppsett = bakgrunnsprosessoppsett(applicationContext = context, isNais = false),
            )
        }

        // `application { }` er lat i testoppsettet, så appen må startes eksplisitt før `app` er satt.
        startApplication()

        // Konstruerer consumeren uten å starte den, slik at meldingsleser-målingene registreres på kontekstens register.
        context.identhendelseConsumer

        app.monitor.raise(ServerReady, app.environment)

        client.get("/metrics").apply {
            status shouldBe HttpStatusCode.OK
            val metrikker = bodyAsText()
            metrikker shouldContain
                """tpts_bakgrunnsprosess_intervall_sekunder{prosess="send-meldekort-jobb",type="jobb"}"""
            metrikker shouldContain
                """tpts_bakgrunnsprosess_sist_vellykket_tidspunkt_sekunder{prosess="send-meldekort-jobb",type="jobb"}"""
            metrikker shouldContain
                """tpts_bakgrunnsprosess_intervall_sekunder{prosess="tpts.identhendelse-v1",type="meldingsleser"}"""
            metrikker shouldContain
                """tpts_bakgrunnsprosess_sist_vellykket_tidspunkt_sekunder{prosess="tpts.identhendelse-v1",type="meldingsleser"}"""
            // Ktor-metrikkene ligger i det samme registeret, som bevis på at det er Ktor-oppsettets register vi skraper.
            metrikker shouldContain "ktor_http_server_requests"
        }

        app.monitor.raise(ApplicationStopping, app)
    }
}
