package no.nav.tiltakspenger.routes

import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
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
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.MeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.tilBrukerDTO
import no.nav.tiltakspenger.meldekort.routes.jacksonSerialization
import no.nav.tiltakspenger.meldekort.routes.meldekort.meldekortRoutes
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

class NesteMeldekortRouteTest {

    @Test
    fun `hent neste meldekort for utfylling`() {
        val tac = TestApplicationContext()

        val førstePeriode = Periode(
            fraOgMed = LocalDate.of(2025, 1, 6),
            tilOgMed = LocalDate.of(2025, 1, 19),
        )

        val førsteMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = førstePeriode,
        )

        val andreMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = Periode(
                fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
            ),
        )

        tac.meldeperiodeRepo.lagre(førsteMeldekort.meldeperiode)
        tac.meldekortRepo.lagre(førsteMeldekort)

        tac.meldeperiodeRepo.lagre(andreMeldekort.meldeperiode)
        tac.meldekortRepo.lagre(andreMeldekort)

        runTest {
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        meldekortRoutes(
                            meldekortService = tac.meldekortService,
                            meldeperiodeService = tac.meldeperiodeService,
                            texasHttpClient = tac.texasHttpClient,
                            clock = tac.clock,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/meldekort/bruker/neste")
                    },
                ).apply {
                    val meldekortFraBody = deserialize<MeldekortTilBrukerDTO>(bodyAsText())

                    status shouldBe HttpStatusCode.OK
                    contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")

                    meldekortFraBody shouldBe førsteMeldekort.tilBrukerDTO()
                }
            }
        }
    }

    @Test
    fun `hent siste innsendte meldekort når ingen er klar til utfylling`() {
        val tac = TestApplicationContext()

        val førstePeriode = ObjectMother.periode(LocalDate.now().minusWeeks(2))

        val innsendtMeldekort = ObjectMother.meldekort(
            mottatt = førstePeriode.tilOgMed.atStartOfDay(),
            periode = førstePeriode,
        )

        val ikkeKlartMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = Periode(
                fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
            ),
        )

        tac.meldeperiodeRepo.lagre(innsendtMeldekort.meldeperiode)
        tac.meldekortRepo.lagre(innsendtMeldekort)

        tac.meldeperiodeRepo.lagre(ikkeKlartMeldekort.meldeperiode)
        tac.meldekortRepo.lagre(ikkeKlartMeldekort)

        runTest {
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        meldekortRoutes(
                            meldekortService = tac.meldekortService,
                            meldeperiodeService = tac.meldeperiodeService,
                            texasHttpClient = tac.texasHttpClient,
                            clock = tac.clock,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/meldekort/bruker/neste")
                    },
                ).apply {
                    val meldekortFraBody = deserialize<MeldekortTilBrukerDTO>(bodyAsText())

                    status shouldBe HttpStatusCode.OK
                    contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")

                    meldekortFraBody shouldBe innsendtMeldekort.tilBrukerDTO()
                }
            }
        }
    }

    @Test
    fun `returner not found dersom ingen meldekort er klare til utfylling eller tidligere utfylt`() {
        val tac = TestApplicationContext()

        val ikkeKlartMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = ObjectMother.periode(LocalDate.now()),
        )

        tac.meldeperiodeRepo.lagre(ikkeKlartMeldekort.meldeperiode)
        tac.meldekortRepo.lagre(ikkeKlartMeldekort)

        runTest {
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        meldekortRoutes(
                            meldekortService = tac.meldekortService,
                            meldeperiodeService = tac.meldeperiodeService,
                            texasHttpClient = tac.texasHttpClient,
                            clock = tac.clock,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Get,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/meldekort/bruker/neste")
                    },
                ).apply {
                    status shouldBe HttpStatusCode.NotFound
                    bodyAsText() shouldBe ""
                }
            }
        }
    }
}
