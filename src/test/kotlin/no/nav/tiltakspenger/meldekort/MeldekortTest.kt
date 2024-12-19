package no.nav.tiltakspenger.meldekort

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.meldekort.routes.jacksonSerialization
import no.nav.tiltakspenger.meldekort.routes.meldekort.MeldekortTilUtfyllingDTO
import no.nav.tiltakspenger.meldekort.routes.meldekort.meldekortRoutes
import no.nav.tiltakspenger.routes.defaultRequest
import org.junit.jupiter.api.Test

internal class MeldekortTest {

    @Test
    fun `kan generere og hente ned generert meldekort`() {
        runTest {
            with(TestApplicationContext()) {
                val tac = this

                testApplication {
                    application {
                        jacksonSerialization()
                        routing {
                            meldekortRoutes(
                                meldekortService = meldekortService,
                                texasHttpClient = tac.texasHttpClient,
                            )
                        }
                    }
                    defaultRequest(
                        HttpMethod.Get,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("/meldekort/bruker/generer")
                        }
                    ).apply {
                        withClue(
                            "Response details:\n" +
                                    "Status: ${this.status}\n" +
                                    "Content-Type: ${this.contentType()}\n" +
                                    "Body: ${this.bodyAsText()}\n",
                        ) {
                            status shouldBe HttpStatusCode.OK
                        }
                    }
                    defaultRequest(
                        HttpMethod.Get,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("/meldekort/bruker/alle")
                        }
                    ).apply {
                        withClue(
                            "Response details:\n" +
                                    "Status: ${this.status}\n" +
                                    "Content-Type: ${this.contentType()}\n" +
                                    "Body: ${this.bodyAsText()}\n",
                        ) {
                            status shouldBe HttpStatusCode.OK
                            val body = deserialize<List<MeldekortTilUtfyllingDTO>>(bodyAsText())
                            body.size shouldBe 1
                        }
                    }
                }
            }
        }
    }

}