package no.nav.tiltakspenger.routes

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatusDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagTilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatus
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.MeldekortKorrigertDagDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.routes.requests.alleBrukersMeldekort
import no.nav.tiltakspenger.routes.requests.hentFørsteMeldekortFraAlleMeldekort
import no.nav.tiltakspenger.routes.requests.hentSisteMeldekortFraAlleMeldekort
import no.nav.tiltakspenger.routes.requests.korrigerMeldekort
import no.nav.tiltakspenger.routes.requests.meldekortBrukerRequest
import no.nav.tiltakspenger.routes.requests.verifiserAntallMeldekortFraAlleMeldekort
import no.nav.tiltakspenger.routes.requests.verifiserKunEtMeldekortFraAlleMeldekort
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortBrukerRouteTest {

    @Test
    fun `hent neste meldekort for utfylling`() {
        withTestApplicationContext(clock = TikkendeKlokke(fixedClockAt(1.mars(2025)))) { tac ->
            val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val andrePeriode = førstePeriode.plus14Dager()

            val førsteMeldeperiode = ObjectMother.meldeperiode(periode = førstePeriode)
            val andreMeldeperiode = ObjectMother.meldeperiode(periode = andrePeriode)

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
            tac.meldekortRepo.lagre(førsteMeldekort)

            tac.meldeperiodeRepo.lagre(andreMeldekort.meldeperiode)
            tac.meldekortRepo.lagre(andreMeldekort)

            val body = meldekortBrukerRequest()!!

            body.nesteMeldekort shouldBe førsteMeldekort.tilMeldekortTilBrukerDTO(clock = tac.clock)
            body.nesteMeldekort!!.status shouldBe MeldekortStatusDTO.KAN_UTFYLLES
            body.forrigeMeldekort shouldBe null
            body.harSoknadUnderBehandling shouldBe false
        }
    }

    @Test
    fun `hent ikke klart meldekort og forrige innsendte meldekort`() {
        withTestApplicationContext { tac ->
            val førstePeriode = ObjectMother.periode(LocalDate.now().minusWeeks(2))
            val andrePeriode = førstePeriode.plus14Dager()

            val førsteMeldeperiode = ObjectMother.meldeperiode(periode = førstePeriode)
            val andreMeldeperiode = ObjectMother.meldeperiode(periode = andrePeriode)

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
            tac.meldekortRepo.lagre(innsendtMeldekort)

            tac.meldeperiodeRepo.lagre(ikkeKlartMeldekort.meldeperiode)
            tac.meldekortRepo.lagre(ikkeKlartMeldekort)

            val body = meldekortBrukerRequest()!!

            body.nesteMeldekort shouldBe ikkeKlartMeldekort.tilMeldekortTilBrukerDTO(tac.clock)
            body.nesteMeldekort!!.status shouldBe MeldekortStatusDTO.IKKE_KLAR

            body.forrigeMeldekort shouldBe innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock)
            body.forrigeMeldekort!!.status shouldBe MeldekortStatusDTO.INNSENDT

            body.harSoknadUnderBehandling shouldBe false
        }
    }

    @Test
    fun `returner ingen meldekort klare til utfylling eller tidligere utfylt`() {
        withTestApplicationContext { tac ->
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
            tac.meldekortRepo.lagre(ikkeKlartMeldekort)

            val body = meldekortBrukerRequest()!!

            body.nesteMeldekort shouldBe ikkeKlartMeldekort.tilMeldekortTilBrukerDTO(tac.clock)
            body.nesteMeldekort!!.status shouldBe MeldekortStatusDTO.IKKE_KLAR

            body.forrigeMeldekort shouldBe null
        }
    }

    @Test
    fun `har ingen meldekort, men søknad under behandling - harSoknadUnderBehandling er true`() {
        withTestApplicationContext { tac ->
            val sak = ObjectMother.sak(
                harSoknadUnderBehandling = true,
                arenaMeldekortStatus = ArenaMeldekortStatus.HAR_IKKE_MELDEKORT,
            )

            tac.sakRepo.lagre(sak)

            val body = meldekortBrukerRequest()!!

            body.nesteMeldekort shouldBe null
            body.forrigeMeldekort shouldBe null
            body.arenaMeldekortStatus shouldBe ArenaMeldekortStatusDTO.HAR_IKKE_MELDEKORT
            body.harSoknadUnderBehandling shouldBe true
        }
    }

    @Test
    fun `skal hente siste versjon av meldekort for en periode med flere versjoner`() {
        withTestApplicationContext(clock = TikkendeKlokke(fixedClockAt(1.mars(2025)))) { tac ->
            val lagreFraSaksbehandlingService = tac.lagreFraSaksbehandlingService

            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

            val førsteDto = meldeperiodeDto(periode = periode)
            val andreDto = førsteDto.copy(id = MeldeperiodeId.random().toString(), versjon = 2)

            val sakDto = ObjectMother.sakDTO(
                meldeperioder = listOf(førsteDto, andreDto),
            )

            lagreFraSaksbehandlingService.lagre(sakDto)

            val body = meldekortBrukerRequest()!!

            body.nesteMeldekort!!.meldeperiodeId shouldBe andreDto.id
            body.nesteMeldekort.versjon shouldBe 2
            body.nesteMeldekort.status shouldBe MeldekortStatusDTO.KAN_UTFYLLES

            body.forrigeMeldekort shouldBe null
        }
    }

    @Test
    fun `skal ikke hente deaktivert meldekort`() {
        withTestApplicationContext { tac ->
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
                antallDagerForPeriode = 0,
            )

            val sakDto = ObjectMother.sakDTO(
                meldeperioder = listOf(førsteDto, andreDto),
            )

            lagreFraSaksbehandlingService.lagre(sakDto)

            val body = meldekortBrukerRequest()!!

            body.nesteMeldekort shouldBe null
            body.forrigeMeldekort shouldBe null
        }
    }

    @Test
    fun `request mangler token - returnerer Unauthorized`() {
        withTestApplicationContext { tac ->
            val sak = ObjectMother.sak(
                harSoknadUnderBehandling = true,
                arenaMeldekortStatus = ArenaMeldekortStatus.HAR_IKKE_MELDEKORT,
            )

            tac.sakRepo.lagre(sak)

            meldekortBrukerRequest(
                jwt = null,
                forventetStatus = HttpStatusCode.Unauthorized,
                forventetBody = "",
                forventetContentType = null,
            )
        }
    }

    @Test
    fun `korrigerer et meldekort`() {
        withTestApplicationContext(clock = TikkendeKlokke(fixedClockAt(1.mars(2025)))) { tac ->
            val førsteMeldeperiode =
                ObjectMother.meldeperiode(periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025)))

            val sak = ObjectMother.sak(meldeperioder = listOf(førsteMeldeperiode))

            val innsendtMeldekort = ObjectMother.meldekort(
                sakId = sak.id,
                mottatt = førsteMeldeperiode.periode.tilOgMed.atStartOfDay(),
                periode = førsteMeldeperiode.periode,
            )

            tac.sakRepo.lagre(sak)

            tac.meldeperiodeRepo.lagre(innsendtMeldekort.meldeperiode)
            tac.meldekortRepo.lagre(innsendtMeldekort)

            val alleMeldekort = this.alleBrukersMeldekort(forventetBody = null)
            verifiserKunEtMeldekortFraAlleMeldekort(alleMeldekort!!)
            val meldekortSomSkalKorrigeres = alleMeldekort.hentFørsteMeldekortFraAlleMeldekort()
            val idForMeldekortSomSkalKorrigeres = MeldekortId.fromString(meldekortSomSkalKorrigeres.id)
            val meldeperiodeIdForMeldekortSomSkalKorrigeres = meldekortSomSkalKorrigeres.meldeperiodeId

            val responseBody = this.korrigerMeldekort(
                meldekortId = idForMeldekortSomSkalKorrigeres.toString(),
                requestBody = """
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
                              "status": "FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU"
                            },
                            {
                              "dato": "2025-01-11",
                              "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                            },
                            {
                              "dato": "2025-01-12",
                              "status": "IKKE_RETT_TIL_TILTAKSPENGER"
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
                              "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                            },
                            {
                              "dato": "2025-01-19",
                              "status": "IKKE_RETT_TIL_TILTAKSPENGER"
                            }
                        ]
                """.trimIndent(),
                locale = "nb",
            )

            val korrigertMeldekort = responseBody!!

            val alleMeldekortEtterKorrigering = this.alleBrukersMeldekort(forventetBody = null)!!
            alleMeldekortEtterKorrigering.verifiserAntallMeldekortFraAlleMeldekort(2)
            val korrigerteMeldekortFraAlleMeldekort = alleMeldekortEtterKorrigering.hentSisteMeldekortFraAlleMeldekort()
            korrigerteMeldekortFraAlleMeldekort.id shouldNotBe idForMeldekortSomSkalKorrigeres.toString()

            tac.meldekortRepo.hentForMeldekortId(idForMeldekortSomSkalKorrigeres, Fnr.fromString(FAKE_FNR))?.status(
                tac.clock,
            ) shouldBe MeldekortStatus.INNSENDT

            korrigertMeldekort.id shouldNotBe idForMeldekortSomSkalKorrigeres
            korrigertMeldekort.status shouldBe MeldekortStatusDTO.INNSENDT
            korrigertMeldekort.meldeperiodeId shouldBe meldeperiodeIdForMeldekortSomSkalKorrigeres
            korrigertMeldekort.fraOgMed shouldBe førsteMeldeperiode.periode.fraOgMed
            korrigertMeldekort.tilOgMed shouldBe førsteMeldeperiode.periode.tilOgMed
            korrigertMeldekort.dager.size shouldBe 14
            korrigertMeldekort.dager shouldBe listOf(
                MeldekortDagTilBrukerDTO(dag = 6.januar(2025), status = MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET),
                MeldekortDagTilBrukerDTO(dag = 7.januar(2025), status = MeldekortDagStatusDTO.DELTATT_MED_LØNN_I_TILTAKET),
                MeldekortDagTilBrukerDTO(dag = 8.januar(2025), status = MeldekortDagStatusDTO.FRAVÆR_SYK),
                MeldekortDagTilBrukerDTO(dag = 9.januar(2025), status = MeldekortDagStatusDTO.FRAVÆR_SYKT_BARN),
                MeldekortDagTilBrukerDTO(dag = 10.januar(2025), status = MeldekortDagStatusDTO.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU),
                MeldekortDagTilBrukerDTO(dag = 11.januar(2025), status = MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER),
                MeldekortDagTilBrukerDTO(dag = 12.januar(2025), status = MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER),
                MeldekortDagTilBrukerDTO(dag = 13.januar(2025), status = MeldekortDagStatusDTO.FRAVÆR_ANNET),
                MeldekortDagTilBrukerDTO(dag = 14.januar(2025), status = MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET),
                MeldekortDagTilBrukerDTO(dag = 15.januar(2025), status = MeldekortDagStatusDTO.DELTATT_MED_LØNN_I_TILTAKET),
                MeldekortDagTilBrukerDTO(dag = 16.januar(2025), status = MeldekortDagStatusDTO.FRAVÆR_SYK),
                MeldekortDagTilBrukerDTO(dag = 17.januar(2025), status = MeldekortDagStatusDTO.FRAVÆR_SYKT_BARN),
                MeldekortDagTilBrukerDTO(dag = 18.januar(2025), status = MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER),
                MeldekortDagTilBrukerDTO(dag = 19.januar(2025), status = MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER),
            )

            val korrigertMeldekortFraDb = tac.meldekortRepo.hentForMeldekortId(MeldekortId.fromString(korrigertMeldekort.id), Fnr.fromString(FAKE_FNR))
            korrigertMeldekortFraDb?.locale shouldBe "nb"
        }
    }

    @Test
    fun `kan ikke korrigere uten endring på dager`() {
        withTestApplicationContext(clock = TikkendeKlokke(fixedClockAt(1.mars(2025)))) { tac ->
            val førsteMeldeperiode =
                ObjectMother.meldeperiode(periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025)))

            val sak = ObjectMother.sak(meldeperioder = listOf(førsteMeldeperiode))

            val innsendtMeldekort = ObjectMother.meldekort(
                sakId = sak.id,
                mottatt = førsteMeldeperiode.periode.tilOgMed.atStartOfDay(),
                periode = førsteMeldeperiode.periode,
            )

            tac.sakRepo.lagre(sak)

            tac.meldeperiodeRepo.lagre(innsendtMeldekort.meldeperiode)
            tac.meldekortRepo.lagre(innsendtMeldekort)

            val dagerUtenEndring = serialize(
                innsendtMeldekort.dager.map {
                    MeldekortKorrigertDagDTO(
                        dato = it.dag,
                        status = it.status,
                    )
                },
            )

            this.korrigerMeldekort(
                meldekortId = innsendtMeldekort.id.toString(),
                requestBody = dagerUtenEndring,
                locale = "nb",
                forventetStatus = HttpStatusCode.BadRequest,
                forventetBody = """{"melding":"Korrigeringen av meldekortet har ingen endringer - Må endre status på minst en dag.","kode":"kan_ikke_korrigere_uten_endring"}""",
            )

            val alleMeldekortEtterKorrigering = this.alleBrukersMeldekort(forventetBody = null)!!
            alleMeldekortEtterKorrigering.verifiserAntallMeldekortFraAlleMeldekort(1)
        }
    }
}
