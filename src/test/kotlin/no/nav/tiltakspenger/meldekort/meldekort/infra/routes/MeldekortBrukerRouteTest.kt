package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.lagreMeldeperiode
import no.nav.tiltakspenger.lagreSak
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.bruker.infra.routes.ArenaMeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.bruker.infra.routes.hentBrukerMedSakRequest
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContext
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortStatus
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortDagStatusDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortDagTilBrukerDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortMedSisteMeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.MeldekortKorrigertDagDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.korrigerMeldekortRequest
import no.nav.tiltakspenger.meldekort.meldekort.infra.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.meldeperiode.infra.tilMeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.mottak.infra.tilMottattSak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortBrukerRouteTest {

    @Test
    fun `hent neste meldekort for utfylling`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val fnr = tac.nesteFnr()
            val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val andrePeriode = førstePeriode.plus14Dager()

            val førsteMeldeperiode = ObjectMother.meldeperiode(periode = førstePeriode, opprettet = nå(tac.clock))
            val andreMeldeperiode = ObjectMother.meldeperiode(periode = andrePeriode, opprettet = nå(tac.clock))

            val sak = ObjectMother.sak(
                fnr = fnr,
                meldeperioder = listOf(førsteMeldeperiode, andreMeldeperiode),
            )

            val førsteMeldekort = ObjectMother.meldekort(
                fnr = fnr,
                mottatt = null,
                periode = førstePeriode,
                sakId = sak.id,
            )

            val andreMeldekort = ObjectMother.meldekort(
                fnr = fnr,
                mottatt = null,
                periode = andrePeriode,
                sakId = sak.id,
            )

            tac.lagreSak(sak)

            tac.lagreMeldeperiode(førsteMeldekort.meldeperiode)
            tac.meldekortRepo.lagre(førsteMeldekort)

            tac.lagreMeldeperiode(andreMeldekort.meldeperiode)
            tac.meldekortRepo.lagre(andreMeldekort)

            val body = hentBrukerMedSakRequest(fnr = fnr.verdi)!!

            body.nesteMeldekort shouldBe førsteMeldekort.tilMeldekortTilBrukerDTO(clock = tac.clock)
            body.nesteMeldekort!!.status shouldBe MeldekortStatusDTO.KAN_UTFYLLES
            body.forrigeMeldekort shouldBe null
            body.harSoknadUnderBehandling shouldBe false
        }
    }

    @Test
    fun `hent ikke klart meldekort og forrige innsendte meldekort`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()
            val førstePeriode = ObjectMother.periode(LocalDate.now(tac.clock).minusWeeks(2))
            val andrePeriode = førstePeriode.plus14Dager()

            val førsteMeldeperiode = ObjectMother.meldeperiode(periode = førstePeriode, opprettet = nå(tac.clock))
            val andreMeldeperiode = ObjectMother.meldeperiode(periode = andrePeriode, opprettet = nå(tac.clock))

            val sak = ObjectMother.sak(
                fnr = fnr,
                meldeperioder = listOf(førsteMeldeperiode, andreMeldeperiode),
            )
            val innsendtMeldekort = ObjectMother.meldekort(
                fnr = fnr,
                sakId = sak.id,
                mottatt = førstePeriode.tilOgMed.atStartOfDay(),
                periode = førstePeriode,
            )
            val ikkeKlartMeldekort = ObjectMother.meldekort(
                fnr = fnr,
                sakId = sak.id,
                mottatt = null,
                periode = andrePeriode,
            )
            tac.lagreSak(sak)

            tac.lagreMeldeperiode(innsendtMeldekort.meldeperiode)
            tac.meldekortRepo.lagre(innsendtMeldekort)

            tac.lagreMeldeperiode(ikkeKlartMeldekort.meldeperiode)
            tac.meldekortRepo.lagre(ikkeKlartMeldekort)

            val body = hentBrukerMedSakRequest(fnr = fnr.verdi)!!

            body.nesteMeldekort shouldBe ikkeKlartMeldekort.tilMeldekortTilBrukerDTO(tac.clock)
            body.nesteMeldekort!!.status shouldBe MeldekortStatusDTO.IKKE_KLAR

            body.forrigeMeldekort shouldBe innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock)
            body.forrigeMeldekort!!.status shouldBe MeldekortStatusDTO.INNSENDT

            body.harSoknadUnderBehandling shouldBe false
        }
    }

    @Test
    fun `returner ingen meldekort klare til utfylling eller tidligere utfylt`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()
            val periode = ObjectMother.periode(LocalDate.now(tac.clock))

            val meldeperiode = ObjectMother.meldeperiode(
                periode = periode,
                opprettet = nå(tac.clock),
            )

            val sak = ObjectMother.sak(fnr = fnr, meldeperioder = listOf(meldeperiode))

            val ikkeKlartMeldekort = ObjectMother.meldekort(
                fnr = fnr,
                mottatt = null,
                periode = periode,
            )

            tac.lagreSak(sak)

            tac.lagreMeldeperiode(ikkeKlartMeldekort.meldeperiode)
            tac.meldekortRepo.lagre(ikkeKlartMeldekort)

            val body = hentBrukerMedSakRequest(fnr = fnr.verdi)!!

            body.nesteMeldekort shouldBe ikkeKlartMeldekort.tilMeldekortTilBrukerDTO(tac.clock)
            body.nesteMeldekort!!.status shouldBe MeldekortStatusDTO.IKKE_KLAR

            body.forrigeMeldekort shouldBe null
        }
    }

    @Test
    fun `har ingen meldekort, men søknad under behandling - harSoknadUnderBehandling er true`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()
            val sak = ObjectMother.sak(
                fnr = fnr,
                harSoknadUnderBehandling = true,
                arenaMeldekortStatus = ArenaMeldekortStatus.HAR_IKKE_MELDEKORT,
            )

            tac.lagreSak(sak)

            val body = hentBrukerMedSakRequest(fnr = fnr.verdi)!!

            body.nesteMeldekort shouldBe null
            body.forrigeMeldekort shouldBe null
            body.arenaMeldekortStatus shouldBe ArenaMeldekortStatusDTO.HAR_IKKE_MELDEKORT
            body.harSoknadUnderBehandling shouldBe true
        }
    }

    @Test
    fun `skal hente siste versjon av meldekort for en periode med flere versjoner`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val fnr = tac.nesteFnr()
            val mottakFraSaksbehandlingService = tac.mottakFraSaksbehandlingService

            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

            val førsteDto = meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))
            val andreDto =
                førsteDto.copy(id = MeldeperiodeId.random().toString(), versjon = 2, opprettet = nå(tac.clock))

            val sakDto = ObjectMother.sakDTO(
                fnr = fnr.verdi,
                meldeperioder = listOf(førsteDto, andreDto),
            )

            mottakFraSaksbehandlingService.lagre(sakDto.tilMottattSak())

            val body = hentBrukerMedSakRequest(fnr = fnr.verdi)!!

            body.nesteMeldekort!!.meldeperiodeId shouldBe andreDto.id
            body.nesteMeldekort.versjon shouldBe 2
            body.nesteMeldekort.status shouldBe MeldekortStatusDTO.KAN_UTFYLLES

            body.forrigeMeldekort shouldBe null
        }
    }

    @Test
    fun `skal ikke hente deaktivert meldekort`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()
            val mottakFraSaksbehandlingService = tac.mottakFraSaksbehandlingService

            val periode = Periode(
                fraOgMed = LocalDate.of(2025, 1, 6),
                tilOgMed = LocalDate.of(2025, 1, 19),
            )

            val førsteDto = meldeperiodeDto(
                periode = periode,
                opprettet = nå(tac.clock),
            )

            val andreDto = førsteDto.copy(
                id = MeldeperiodeId.random().toString(),
                versjon = 2,
                opprettet = førsteDto.opprettet.plusSeconds(1),
                girRett = periode.tilDager().associateWith { false },
                antallDagerForPeriode = 0,
            )

            val sakDto = ObjectMother.sakDTO(
                fnr = fnr.verdi,
                meldeperioder = listOf(førsteDto, andreDto),
            )

            mottakFraSaksbehandlingService.lagre(sakDto.tilMottattSak())

            val body = hentBrukerMedSakRequest(fnr = fnr.verdi)!!

            body.nesteMeldekort shouldBe null
            body.forrigeMeldekort shouldBe null
        }
    }

    @Test
    fun `request mangler token - returnerer Unauthorized`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()
            val sak = ObjectMother.sak(
                fnr = fnr,
                harSoknadUnderBehandling = true,
                arenaMeldekortStatus = ArenaMeldekortStatus.HAR_IKKE_MELDEKORT,
            )

            tac.lagreSak(sak)

            hentBrukerMedSakRequest(
                fnr = fnr.verdi,
                jwt = null,
                forventetStatus = HttpStatusCode.Unauthorized,
                forventetBody = "",
                forventetContentType = null,
            )
        }
    }

    @Test
    fun `korrigerer et meldekort`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()
            val førsteMeldeperiode =
                ObjectMother.meldeperiode(
                    periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025)),
                    opprettet = nå(tac.clock),
                )

            val sak = ObjectMother.sak(fnr = fnr, meldeperioder = listOf(førsteMeldeperiode))

            val innsendtMeldekort = ObjectMother.meldekort(
                fnr = fnr,
                sakId = sak.id,
                mottatt = førsteMeldeperiode.periode.tilOgMed.atStartOfDay(),
                periode = førsteMeldeperiode.periode,
            )

            tac.lagreSak(sak)

            tac.lagreMeldeperiode(innsendtMeldekort.meldeperiode)
            tac.meldekortRepo.lagre(innsendtMeldekort)

            this.hentAlleInnsendteMeldekortRequest(fnr = fnr.verdi, forventetBody = null)!!.shouldBeAlleMeldekortJson(
                forrigeMeldekort = innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock),
                meldekortMedSisteMeldeperiode = listOf(
                    MeldekortMedSisteMeldeperiodeDTO(
                        meldekort = innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock),
                        sisteMeldeperiode = innsendtMeldekort.meldeperiode.tilMeldeperiodeDTO(),
                    ),
                ),
            )
            val idForMeldekortSomSkalKorrigeres = innsendtMeldekort.id
            val meldeperiodeIdForMeldekortSomSkalKorrigeres = innsendtMeldekort.meldeperiode.id.toString()

            val responseBody = this.korrigerMeldekortRequest(
                tac = tac,
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
                fnr = fnr.verdi,
            )

            val korrigertMeldekort = responseBody!!

            this.hentAlleInnsendteMeldekortRequest(fnr = fnr.verdi, forventetBody = null)!!.shouldBeAlleMeldekortJson(
                forrigeMeldekort = innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock),
                meldekortMedSisteMeldeperiode = listOf(
                    MeldekortMedSisteMeldeperiodeDTO(
                        meldekort = innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock),
                        sisteMeldeperiode = innsendtMeldekort.meldeperiode.tilMeldeperiodeDTO(),
                    ),
                    MeldekortMedSisteMeldeperiodeDTO(
                        meldekort = korrigertMeldekort,
                        sisteMeldeperiode = innsendtMeldekort.meldeperiode.tilMeldeperiodeDTO(),
                    ),
                ),
            )
            tac.meldekortRepo.hentForMeldekortId(idForMeldekortSomSkalKorrigeres, fnr)?.status(
                tac.clock,
            ) shouldBe MeldekortStatus.INNSENDT

            korrigertMeldekort.id shouldNotBe idForMeldekortSomSkalKorrigeres
            korrigertMeldekort.status shouldBe MeldekortStatusDTO.INNSENDT
            korrigertMeldekort.meldeperiodeId shouldBe meldeperiodeIdForMeldekortSomSkalKorrigeres
            korrigertMeldekort.fraOgMed shouldBe førsteMeldeperiode.periode.fraOgMed
            korrigertMeldekort.tilOgMed shouldBe førsteMeldeperiode.periode.tilOgMed
            korrigertMeldekort.dager.size shouldBe 14
            korrigertMeldekort.dager shouldBe listOf(
                MeldekortDagTilBrukerDTO(
                    dag = 6.januar(2025),
                    status = MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET,
                ),
                MeldekortDagTilBrukerDTO(
                    dag = 7.januar(2025),
                    status = MeldekortDagStatusDTO.DELTATT_MED_LØNN_I_TILTAKET,
                ),
                MeldekortDagTilBrukerDTO(dag = 8.januar(2025), status = MeldekortDagStatusDTO.FRAVÆR_SYK),
                MeldekortDagTilBrukerDTO(dag = 9.januar(2025), status = MeldekortDagStatusDTO.FRAVÆR_SYKT_BARN),
                MeldekortDagTilBrukerDTO(
                    dag = 10.januar(2025),
                    status = MeldekortDagStatusDTO.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU,
                ),
                MeldekortDagTilBrukerDTO(
                    dag = 11.januar(2025),
                    status = MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER,
                ),
                MeldekortDagTilBrukerDTO(
                    dag = 12.januar(2025),
                    status = MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER,
                ),
                MeldekortDagTilBrukerDTO(dag = 13.januar(2025), status = MeldekortDagStatusDTO.FRAVÆR_ANNET),
                MeldekortDagTilBrukerDTO(
                    dag = 14.januar(2025),
                    status = MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET,
                ),
                MeldekortDagTilBrukerDTO(
                    dag = 15.januar(2025),
                    status = MeldekortDagStatusDTO.DELTATT_MED_LØNN_I_TILTAKET,
                ),
                MeldekortDagTilBrukerDTO(dag = 16.januar(2025), status = MeldekortDagStatusDTO.FRAVÆR_SYK),
                MeldekortDagTilBrukerDTO(dag = 17.januar(2025), status = MeldekortDagStatusDTO.FRAVÆR_SYKT_BARN),
                MeldekortDagTilBrukerDTO(
                    dag = 18.januar(2025),
                    status = MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER,
                ),
                MeldekortDagTilBrukerDTO(
                    dag = 19.januar(2025),
                    status = MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER,
                ),
            )

            val korrigertMeldekortFraDb = tac.meldekortRepo.hentForMeldekortId(
                MeldekortId.fromString(korrigertMeldekort.id),
                fnr,
            )
            korrigertMeldekortFraDb?.locale shouldBe "nb"
        }
    }

    @Test
    fun `kan ikke korrigere uten endring på dager`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()
            val førsteMeldeperiode =
                ObjectMother.meldeperiode(
                    periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025)),
                    opprettet = nå(tac.clock),
                )

            val sak = ObjectMother.sak(fnr = fnr, meldeperioder = listOf(førsteMeldeperiode))

            val innsendtMeldekort = ObjectMother.meldekort(
                fnr = fnr,
                sakId = sak.id,
                mottatt = førsteMeldeperiode.periode.tilOgMed.atStartOfDay(),
                periode = førsteMeldeperiode.periode,
            )

            tac.lagreSak(sak)

            tac.lagreMeldeperiode(innsendtMeldekort.meldeperiode)
            tac.meldekortRepo.lagre(innsendtMeldekort)

            val dagerUtenEndring =
                innsendtMeldekort.dager.map {
                    MeldekortKorrigertDagDTO(
                        dato = it.dag,
                        status = it.status,
                    )
                }

            this.korrigerMeldekortRequest(
                tac = tac,
                meldekortId = innsendtMeldekort.id.toString(),
                requestDto = dagerUtenEndring,
                locale = "nb",
                fnr = fnr.verdi,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetBody = """{"melding":"Korrigeringen av meldekortet har ingen endringer - Må endre status på minst en dag.","kode":"kan_ikke_korrigere_uten_endring"}""",
            )

            this.hentAlleInnsendteMeldekortRequest(fnr = fnr.verdi, forventetBody = null)!!.shouldBeAlleMeldekortJson(
                forrigeMeldekort = innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock),
                meldekortMedSisteMeldeperiode = listOf(
                    MeldekortMedSisteMeldeperiodeDTO(
                        meldekort = innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock),
                        sisteMeldeperiode = innsendtMeldekort.meldeperiode.tilMeldeperiodeDTO(),
                    ),
                ),
            )
        }
    }
}
