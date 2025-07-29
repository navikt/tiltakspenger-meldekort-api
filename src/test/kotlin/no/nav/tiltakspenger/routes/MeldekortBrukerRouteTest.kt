package no.nav.tiltakspenger.routes

import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.domene.BrukerDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.routes.requests.alleBrukersMeldekort
import no.nav.tiltakspenger.routes.requests.hentFørsteMeldekortFraAlleMeldekort
import no.nav.tiltakspenger.routes.requests.korrigerMeldekort
import no.nav.tiltakspenger.routes.requests.verifiserKunEtMeldekortFraAlleMeldekort
import org.json.JSONObject
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortBrukerRouteTest {

    private suspend fun ApplicationTestBuilder.meldekortBrukerRequest() = defaultRequest(
        HttpMethod.Get,
        url {
            protocol = URLProtocol.HTTPS
            path("/brukerfrontend/bruker")
        },
        jwt = JwtGenerator().createJwtForUser(),
    )

    @Test
    fun `hent neste meldekort for utfylling`() {
        val tac = TestApplicationContext()

        val førstePeriode = Periode(
            fraOgMed = LocalDate.of(2025, 1, 6),
            tilOgMed = LocalDate.of(2025, 1, 19),
        )

        val andrePeriode = førstePeriode.plus14Dager()

        val førsteMeldeperiode = ObjectMother.meldeperiode(
            periode = førstePeriode,
        )

        val andreMeldeperiode = ObjectMother.meldeperiode(
            periode = andrePeriode,
        )

        val sak = ObjectMother.sak(
            meldeperioder = listOf(førsteMeldeperiode, andreMeldeperiode),
        )

        val førsteMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = førstePeriode,
            sakId = sak.id,
        )

        val andreMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = andrePeriode,
            sakId = sak.id,
        )

        tac.sakRepo.lagre(sak)

        tac.meldeperiodeRepo.lagre(førsteMeldekort.meldeperiode)
        tac.meldekortRepo.opprett(førsteMeldekort)

        tac.meldeperiodeRepo.lagre(andreMeldekort.meldeperiode)
        tac.meldekortRepo.opprett(andreMeldekort)

        testMedMeldekortRoutes(tac) {
            meldekortBrukerRequest().apply {
                val body = deserialize<BrukerDTO.MedSak>(bodyAsText())

                status shouldBe HttpStatusCode.OK
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")

                body.nesteMeldekort shouldBe førsteMeldekort.tilMeldekortTilBrukerDTO()
                body.nesteMeldekort!!.status shouldBe MeldekortStatusDTO.KAN_UTFYLLES
                body.forrigeMeldekort shouldBe null
                body.harSoknadUnderBehandling shouldBe false
            }
        }
    }

    @Test
    fun `hent ikke klart meldekort og forrige innsendte meldekort`() {
        val tac = TestApplicationContext()

        val førstePeriode = ObjectMother.periode(LocalDate.now().minusWeeks(2))
        val andrePeriode = førstePeriode.plus14Dager()

        val førsteMeldeperiode = ObjectMother.meldeperiode(
            periode = førstePeriode,
        )

        val andreMeldeperiode = ObjectMother.meldeperiode(
            periode = andrePeriode,
        )

        val sak = ObjectMother.sak(
            meldeperioder = listOf(førsteMeldeperiode, andreMeldeperiode),
        )

        val innsendtMeldekort = ObjectMother.meldekort(
            sakId = sak.id,
            mottatt = førstePeriode.tilOgMed.atStartOfDay(),
            periode = førstePeriode,
        )

        val ikkeKlartMeldekort = ObjectMother.meldekort(
            sakId = sak.id,
            mottatt = null,
            periode = andrePeriode,
        )

        tac.sakRepo.lagre(sak)

        tac.meldeperiodeRepo.lagre(innsendtMeldekort.meldeperiode)
        tac.meldekortRepo.opprett(innsendtMeldekort)

        tac.meldeperiodeRepo.lagre(ikkeKlartMeldekort.meldeperiode)
        tac.meldekortRepo.opprett(ikkeKlartMeldekort)

        testMedMeldekortRoutes(tac) {
            meldekortBrukerRequest().apply {
                val body = deserialize<BrukerDTO.MedSak>(bodyAsText())

                status shouldBe HttpStatusCode.OK
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")

                body.nesteMeldekort shouldBe ikkeKlartMeldekort.tilMeldekortTilBrukerDTO()
                body.nesteMeldekort!!.status shouldBe MeldekortStatusDTO.IKKE_KLAR

                body.forrigeMeldekort shouldBe innsendtMeldekort.tilMeldekortTilBrukerDTO()
                body.forrigeMeldekort!!.status shouldBe MeldekortStatusDTO.INNSENDT

                body.harSoknadUnderBehandling shouldBe false
            }
        }
    }

    @Test
    fun `returner ingen meldekort klare til utfylling eller tidligere utfylt`() {
        val tac = TestApplicationContext()

        val periode = ObjectMother.periode(LocalDate.now())

        val meldeperiode = ObjectMother.meldeperiode(
            periode = periode,
        )

        val sak = ObjectMother.sak(meldeperioder = listOf(meldeperiode))

        val ikkeKlartMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = periode,
        )

        tac.sakRepo.lagre(sak)

        tac.meldeperiodeRepo.lagre(ikkeKlartMeldekort.meldeperiode)
        tac.meldekortRepo.opprett(ikkeKlartMeldekort)

        testMedMeldekortRoutes(tac) {
            meldekortBrukerRequest().apply {
                val body = deserialize<BrukerDTO.MedSak>(bodyAsText())

                status shouldBe HttpStatusCode.OK

                body.nesteMeldekort shouldBe ikkeKlartMeldekort.tilMeldekortTilBrukerDTO()
                body.nesteMeldekort!!.status shouldBe MeldekortStatusDTO.IKKE_KLAR

                body.forrigeMeldekort shouldBe null
            }
        }
    }

    @Test
    fun `har ingen meldekort, men søknad under behandling - harSoknadUnderBehandling er true`() {
        val tac = TestApplicationContext()

        val sak = ObjectMother.sak(
            harSoknadUnderBehandling = true,
            arenaMeldekortStatus = ArenaMeldekortStatus.HAR_IKKE_MELDEKORT,
        )

        tac.sakRepo.lagre(sak)

        testMedMeldekortRoutes(tac) {
            meldekortBrukerRequest().apply {
                val body = deserialize<BrukerDTO.MedSak>(bodyAsText())

                status shouldBe HttpStatusCode.OK

                body.nesteMeldekort shouldBe null
                body.forrigeMeldekort shouldBe null
                body.arenaMeldekortStatus shouldBe ArenaMeldekortStatusDTO.HAR_IKKE_MELDEKORT
                body.harSoknadUnderBehandling shouldBe true
            }
        }
    }

    @Test
    fun `skal hente siste versjon av meldekort for en periode med flere versjoner`() {
        val tac = TestApplicationContext()
        val lagreFraSaksbehandlingService = tac.lagreFraSaksbehandlingService

        val periode = Periode(
            fraOgMed = LocalDate.of(2025, 1, 6),
            tilOgMed = LocalDate.of(2025, 1, 19),
        )

        val førsteDto = meldeperiodeDto(
            periode = periode,
        )
        val andreDto = førsteDto.copy(id = MeldeperiodeId.random().toString(), versjon = 2)

        val sakDto = ObjectMother.sakDTO(
            meldeperioder = listOf(førsteDto, andreDto),
        )

        lagreFraSaksbehandlingService.lagre(sakDto)

        testMedMeldekortRoutes(tac) {
            meldekortBrukerRequest().apply {
                val body = deserialize<BrukerDTO.MedSak>(bodyAsText())

                status shouldBe HttpStatusCode.OK

                body.nesteMeldekort!!.meldeperiodeId shouldBe andreDto.id
                body.nesteMeldekort.versjon shouldBe 2
                body.nesteMeldekort.status shouldBe MeldekortStatusDTO.KAN_UTFYLLES

                body.forrigeMeldekort shouldBe null
            }
        }
    }

    @Test
    fun `skal ikke hente deaktivert meldekort`() {
        val tac = TestApplicationContext()
        val lagreFraSaksbehandlingService = tac.lagreFraSaksbehandlingService

        val periode = Periode(
            fraOgMed = LocalDate.of(2025, 1, 6),
            tilOgMed = LocalDate.of(2025, 1, 19),
        )

        val førsteDto = meldeperiodeDto(
            periode = periode,
        )

        val andreDto = førsteDto.copy(
            id = MeldeperiodeId.random().toString(),
            versjon = 2,
            girRett = periode.tilDager().associateWith { false },
        )

        val sakDto = ObjectMother.sakDTO(
            meldeperioder = listOf(førsteDto, andreDto),
        )

        lagreFraSaksbehandlingService.lagre(sakDto)

        testMedMeldekortRoutes(tac) {
            meldekortBrukerRequest().apply {
                val body = deserialize<BrukerDTO.MedSak>(bodyAsText())

                status shouldBe HttpStatusCode.OK

                body.nesteMeldekort shouldBe null
                body.forrigeMeldekort shouldBe null
            }
        }
    }

    @Test
    fun `request mangler token - returnerer Unauthorized`() {
        val tac = TestApplicationContext()

        val sak = ObjectMother.sak(
            harSoknadUnderBehandling = true,
            arenaMeldekortStatus = ArenaMeldekortStatus.HAR_IKKE_MELDEKORT,
        )

        tac.sakRepo.lagre(sak)

        testMedMeldekortRoutes(tac) {
            defaultRequest(
                HttpMethod.Get,
                url {
                    protocol = URLProtocol.HTTPS
                    path("/brukerfrontend/bruker")
                },
                jwt = null,
            ).apply {
                status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }

    @Test
    fun `korrigerer et meldekort`() {
        val tac = TestApplicationContext()
        val lagreFraSaksbehandlingService = tac.lagreFraSaksbehandlingService

        val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

        val førsteDto = meldeperiodeDto(periode = periode)

        val sakDto = ObjectMother.sakDTO(meldeperioder = listOf(førsteDto))

        lagreFraSaksbehandlingService.lagre(sakDto)
        testMedMeldekortRoutes(tac) {
            val alleMeldekort = this.alleBrukersMeldekort()
            verifiserKunEtMeldekortFraAlleMeldekort(alleMeldekort)
            val eksisterendeMeldekortId = MeldekortId.fromString(
                JSONObject(alleMeldekort.hentFørsteMeldekortFraAlleMeldekort()).get("id").toString(),
            )

            val korrigerResponse = this.korrigerMeldekort(
                meldekortId = eksisterendeMeldekortId.toString(),
                dager = """
                        [
                            {
                              "dato": "2025-01-06",
                              "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                            },
                            {
                              "dato": "2025-01-07",
                              "status": "DELTATT_MED_LØNN_I_TILTAKET"
                            },
                            {
                              "dato": "2025-01-08",
                              "status": "FRAVÆR_SYK"
                            },
                            {
                              "dato": "2025-01-09",
                              "status": "FRAVÆR_SYKT_BARN"
                            },
                            {
                              "dato": "2025-01-10",
                              "status": "FRAVÆR_GODKJENT_AV_NAV"
                            },
                            {
                              "dato": "2025-01-11",
                              "status": "IKKE_BESVART"
                            },
                            {
                              "dato": "2025-01-12",
                              "status": "IKKE_BESVART"
                            },
                            {
                              "dato": "2025-01-13",
                              "status": "FRAVÆR_ANNET"
                            },
                            {
                              "dato": "2025-01-14",
                              "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
                            },
                            {
                              "dato": "2025-01-15",
                              "status": "DELTATT_MED_LØNN_I_TILTAKET"
                            },
                            {
                              "dato": "2025-01-16",
                              "status": "FRAVÆR_SYK"
                            },
                            {
                              "dato": "2025-01-17",
                              "status": "FRAVÆR_SYKT_BARN"
                            },
                            {
                              "dato": "2025-01-18",
                              "status": "IKKE_BESVART"
                            },
                            {
                              "dato": "2025-01-19",
                              "status": "IKKE_BESVART"
                            }
                        ]
                """.trimIndent(),
            )

            TODO("Hva har vi lyst til å verifisere her?")
        }
    }
}
