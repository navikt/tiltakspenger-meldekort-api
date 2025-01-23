package no.nav.tiltakspenger.meldekort

internal class MeldekortTest {
/*
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
                                brukersMeldekortService = brukersMeldekortService,
                                meldeperiodeService = meldeperiodeService,
                                texasHttpClient = tac.texasHttpClient,
                            )
                        }
                    }
                    defaultRequest(
                        HttpMethod.Get,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("/meldekort/bruker/generer")
                        },
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
                        },
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
 */
}
