package no.nav.tiltakspenger.meldekort.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.ServerReady
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.TestApplicationContextMedInMemoryDb
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Readiness
import no.nav.tiltakspenger.meldekort.infra.routes.ktorSetup
import org.junit.jupiter.api.Test

/**
 * Den generiske oppstarts-/livssyklus-orkestreringen (idempotent start/stopp, shutdown-race, stoppbarKafkaConsumer, startMedOpprydding, Netty-streng-lås) testes i ktor-common (`OppstartTest`).
 * Her verifiserer vi kun at meldekort-api faktisk wirer felles-mønsteret riktig sammen med sitt eget [ktorSetup] og sine ekte bakgrunnsprosesser.
 */
class ApplicationTest {
    private val log = KotlinLogging.logger { }

    /**
     * Kjører den faktiske meldekort-livssyklusen ([konfigurerMeldekortLivssyklus]) sammen med det ekte [ktorSetup]-oppsettet og de ekte bakgrunnsprosessene (skedulert [no.nav.tiltakspenger.libs.jobber.TaskExecutor]).
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
            konfigurerMeldekortLivssyklus(
                log = log,
                isNais = false,
                applicationContext = context,
                readiness = readiness,
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
}
