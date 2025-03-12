package no.nav.tiltakspenger.routes

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.domene.tilMeldeperiode
import no.nav.tiltakspenger.meldekort.routes.jacksonSerialization
import no.nav.tiltakspenger.meldekort.routes.meldekort.meldekortRoutes
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MottaMeldeperiodeRouteTest {
    private suspend fun ApplicationTestBuilder.mottaMeldeperiodeRequest(dto: MeldeperiodeDTO) = defaultRequest(
        HttpMethod.Post,
        url {
            protocol = URLProtocol.HTTPS
            path("/meldekort")
        },
    ) {
        setBody(serialize(dto))
    }

    @Test
    fun `Skal lagre meldeperioden og opprette meldekort`() {
        val tac = TestApplicationContext()

        val dto = ObjectMother.meldeperiodeDto()
        val id = MeldeperiodeId.fromString(dto.id)

        runTest {
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        meldekortRoutes(
                            meldekortService = tac.meldekortService,
                            meldeperiodeService = tac.meldeperiodeService,
                            texasHttpClient = tac.texasHttpClient,
                        )
                    }
                }

                mottaMeldeperiodeRequest(dto).apply {
                    status shouldBe HttpStatusCode.OK

                    tac.meldeperiodeRepo.hentForId(id) shouldBe dto.tilMeldeperiode().getOrFail()
                    tac.meldekortRepo.hentMeldekortForMeldeperiodeId(id).shouldNotBeNull()
                }
            }
        }
    }

    @Test
    fun `Skal håndtere duplikate requests for lagring av meldeperiode og returnere ok`() {
        val tac = TestApplicationContext()

        val dto = ObjectMother.meldeperiodeDto()
        val id = MeldeperiodeId.fromString(dto.id)

        runTest {
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        meldekortRoutes(
                            meldekortService = tac.meldekortService,
                            meldeperiodeService = tac.meldeperiodeService,
                            texasHttpClient = tac.texasHttpClient,
                        )
                    }
                }

                mottaMeldeperiodeRequest(dto).apply {
                    status shouldBe HttpStatusCode.OK
                }

                mottaMeldeperiodeRequest(dto).apply {
                    status shouldBe HttpStatusCode.OK

                    tac.meldeperiodeRepo.hentForId(id) shouldBe dto.tilMeldeperiode().getOrFail()
                    tac.meldekortRepo.hentMeldekortForMeldeperiodeId(id).shouldNotBeNull()
                }
            }
        }
    }

    @Test
    fun `Skal håndtere requests for lagring av samme meldeperiode med ulike data og returnere 409`() {
        val tac = TestApplicationContext()

        val dto = ObjectMother.meldeperiodeDto()
        val id = MeldeperiodeId.fromString(dto.id)

        val dtoMedDiff = dto.copy(opprettet = dto.opprettet.minusDays(1))

        runTest {
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        meldekortRoutes(
                            meldekortService = tac.meldekortService,
                            meldeperiodeService = tac.meldeperiodeService,
                            texasHttpClient = tac.texasHttpClient,
                        )
                    }
                }

                mottaMeldeperiodeRequest(dto).apply {
                    status shouldBe HttpStatusCode.OK
                }

                mottaMeldeperiodeRequest(dtoMedDiff).apply {
                    status shouldBe HttpStatusCode.Conflict

                    val meldeperiode = tac.meldeperiodeRepo.hentForId(id)!!

                    meldeperiode shouldBe dto.tilMeldeperiode().getOrFail()
                    meldeperiode.opprettet shouldBe dto.opprettet

                    tac.meldekortRepo.hentMeldekortForMeldeperiodeId(id).shouldNotBeNull()
                }
            }
        }
    }
}
