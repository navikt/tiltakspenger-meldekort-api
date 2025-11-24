package no.nav.tiltakspenger.routes

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.dato.november
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode.Companion.kanFyllesUtFraOgMed
import no.nav.tiltakspenger.meldekort.domene.tilSak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.lagreMeldekortFraBrukerKommando
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class MottaSakerRouteTest {
    private suspend fun ApplicationTestBuilder.mottaSakRequest(dto: SakTilMeldekortApiDTO) = defaultRequest(
        HttpMethod.Post,
        url {
            protocol = URLProtocol.HTTPS
            path("/saksbehandling/sak")
        },
        jwt = JwtGenerator().createJwtForSystembruker(),
    ) {
        setBody(serialize(dto))
    }

    private val førstePeriode = Periode(
        fraOgMed = LocalDate.of(2025, 1, 6),
        tilOgMed = LocalDate.of(2025, 1, 19),
    )

    @Test
    fun `Skal lagre saken og opprette meldekort`() {
        val tac = TestApplicationContext()
        (tac.clock as TikkendeKlokke).spolTil(1.mars(2025))

        val sakDto = ObjectMother.sakDTO(
            meldeperioder = listOf(
                ObjectMother.meldeperiodeDto(periode = førstePeriode),
                ObjectMother.meldeperiodeDto(periode = førstePeriode.plus14Dager()),
            ),
        )

        val id = SakId.fromString(sakDto.sakId)

        testMedMeldekortRoutes(tac) {
            mottaSakRequest(sakDto).apply {
                status shouldBe HttpStatusCode.OK

                val sak = tac.sakRepo.hent(id)

                sak shouldBe sakDto.tilSak()
                sak!!.meldeperioder.size shouldBe 2
                sak.harSoknadUnderBehandling shouldBe false

                tac.meldekortRepo.hentInnsendteMeldekortForBruker(sak.fnr).size shouldBe 2
            }
        }
    }

    @Test
    fun `Skal oppdatere sak hvis harSoknadUnderBehandling endres`() {
        val tac = TestApplicationContext()

        val lagretSak = ObjectMother.sak(harSoknadUnderBehandling = false)
        tac.sakRepo.lagre(lagretSak)

        val id = lagretSak.id
        val sakDto = ObjectMother.sakDTO(
            sakId = id.toString(),
            saksnummer = lagretSak.saksnummer,
            fnr = lagretSak.fnr.verdi,
            meldeperioder = emptyList(),
            harSoknadUnderBehandling = true,
        )

        testMedMeldekortRoutes(tac) {
            mottaSakRequest(sakDto).apply {
                status shouldBe HttpStatusCode.OK

                val sak = tac.sakRepo.hent(id)

                sak shouldBe sakDto.tilSak()
                sak!!.meldeperioder.size shouldBe 0
                sak.harSoknadUnderBehandling shouldBe true
            }
        }
    }

    @Test
    fun `Skal håndtere duplikate requests for lagring av sak og returnere ok`() {
        val tac = TestApplicationContext()

        val sakDto = ObjectMother.sakDTO(
            meldeperioder = listOf(
                ObjectMother.meldeperiodeDto(periode = førstePeriode),
                ObjectMother.meldeperiodeDto(periode = førstePeriode.plus14Dager()),
            ),
        )

        testMedMeldekortRoutes(tac) {
            mottaSakRequest(sakDto).apply {
                status shouldBe HttpStatusCode.OK
            }

            mottaSakRequest(sakDto).apply {
                status shouldBe HttpStatusCode.OK

                tac.sakRepo.hent(SakId.fromString(sakDto.sakId)) shouldBe sakDto.tilSak()
            }
        }
    }

    @Test
    fun `Skal håndtere oppdatering av sak med nye meldeperioder`() {
        val tac = TestApplicationContext()

        val førsteMeldeperiode = ObjectMother.meldeperiodeDto(
            periode = førstePeriode,
            opprettet = LocalDateTime.of(2025, 1, 5, 12, 0),
        )

        val andreMeldeperiode = ObjectMother.meldeperiodeDto(periode = førstePeriode.plus14Dager())

        val sakDto1 = ObjectMother.sakDTO(
            meldeperioder = listOf(
                førsteMeldeperiode,
            ),
        )

        val sakDto2 = sakDto1.copy(
            meldeperioder = listOf(
                førsteMeldeperiode,
                andreMeldeperiode,
            ),
        )

        testMedMeldekortRoutes(tac) {
            mottaSakRequest(sakDto1).apply {
                status shouldBe HttpStatusCode.OK
            }

            mottaSakRequest(sakDto2).apply {
                status shouldBe HttpStatusCode.OK

                val sak = tac.sakRepo.hent(SakId.fromString(sakDto2.sakId))!!

                sak.meldeperioder.size shouldBe 2
            }
        }
    }

    @Test
    fun `Skal returnere 409 ved lagring av sak med ulike meldeperioder med samme meldeperiode-id`() {
        val tac = TestApplicationContext()

        val førsteMeldeperiode = ObjectMother.meldeperiodeDto(
            periode = førstePeriode,
            opprettet = LocalDateTime.of(2025, 1, 5, 12, 0),
        )

        val andreMeldeperiode = ObjectMother.meldeperiodeDto(periode = førstePeriode.plus14Dager())

        val sakDto1 = ObjectMother.sakDTO(
            meldeperioder = listOf(
                førsteMeldeperiode,
                andreMeldeperiode,
            ),
        )

        val sakDto2 = sakDto1.copy(
            meldeperioder = listOf(
                førsteMeldeperiode.copy(opprettet = LocalDateTime.of(2025, 1, 5, 13, 0)),
                andreMeldeperiode,
            ),
        )
        testMedMeldekortRoutes(tac) {
            mottaSakRequest(sakDto1).apply {
                status shouldBe HttpStatusCode.OK
            }

            mottaSakRequest(sakDto2).apply {
                status shouldBe HttpStatusCode.Conflict

                val meldeperiode = tac.meldeperiodeRepo.hentForId(MeldeperiodeId.fromString(førsteMeldeperiode.id))!!

                meldeperiode.opprettet shouldBe førsteMeldeperiode.opprettet
            }
        }
    }

    @Test
    fun `Skal opprette nytt meldekort og deaktivere forrige ved ny meldeperiode-versjon`() {
        val tac = TestApplicationContext()

        val meldeperiode = ObjectMother.meldeperiodeDto(
            periode = førstePeriode,
        )
        val nyMeldeperiodeVersjon = ObjectMother.meldeperiodeDto(

            periode = førstePeriode,
            versjon = 2,
        )

        val sakDto1 = ObjectMother.sakDTO(
            meldeperioder = listOf(meldeperiode),
        )
        val sakDto2 = sakDto1.copy(
            meldeperioder = listOf(nyMeldeperiodeVersjon),
        )

        testMedMeldekortRoutes(tac) {
            mottaSakRequest(sakDto1).apply {
                status shouldBe HttpStatusCode.OK
            }

            mottaSakRequest(sakDto2).apply {
                status shouldBe HttpStatusCode.OK

                val (førsteMeldekort, andreMeldekort) =
                    tac.meldekortRepo.hentMeldekortForKjedeId(
                        MeldeperiodeKjedeId(meldeperiode.kjedeId),
                        Fnr.fromString(sakDto1.fnr),
                    )

                førsteMeldekort.deaktivert shouldNotBe null
                førsteMeldekort.erVarselInaktivert shouldBe true

                andreMeldekort.deaktivert shouldBe null
                andreMeldekort.erVarselInaktivert shouldBe false
            }
        }
    }

    @Test
    fun `Skal ikke opprette nytt meldekort for kjede der meldekort allerede er mottatt`() {
        val tac = TestApplicationContext()
        (tac.clock as TikkendeKlokke).spolTil(1.mars(2025))

        val meldeperiode = ObjectMother.meldeperiodeDto(
            periode = førstePeriode,
        )
        val nyMeldeperiodeVersjon = ObjectMother.meldeperiodeDto(
            periode = førstePeriode,
            versjon = 2,
        )

        val sakDto1 = ObjectMother.sakDTO(
            meldeperioder = listOf(meldeperiode),
        )
        val sakDto2 = sakDto1.copy(
            meldeperioder = listOf(nyMeldeperiodeVersjon),
        )

        testMedMeldekortRoutes(tac) {
            mottaSakRequest(sakDto1).apply {
                status shouldBe HttpStatusCode.OK

                val meldekort = tac.meldekortRepo.hentMeldekortForKjedeId(
                    MeldeperiodeKjedeId(meldeperiode.kjedeId),
                    Fnr.fromString(sakDto1.fnr),
                ).first()

                val meldeperiode = meldekort.meldeperiode

                val lagreKommando = lagreMeldekortFraBrukerKommando(
                    meldeperiode = meldeperiode,
                    meldekortId = meldekort.id,
                )

                tac.meldekortRepo.lagre(
                    meldekort.fyllUtMeldekortFraBruker(
                        sisteMeldeperiode = meldeperiode,
                        clock = tac.clock,
                        brukerutfylteDager = lagreKommando.dager.map { it.tilMeldekortDag() },
                        korrigering = false,
                    ),
                )
            }

            mottaSakRequest(sakDto2).apply {
                status shouldBe HttpStatusCode.OK

                val meldekortFraKjede = tac.meldekortRepo.hentMeldekortForKjedeId(
                    MeldeperiodeKjedeId(meldeperiode.kjedeId),
                    Fnr.fromString(sakDto1.fnr),
                )

                meldekortFraKjede.size shouldBe 1
            }
        }
    }

    @Nested
    inner class FinnerNærmesteFredagInnenforPeriodenOgLeggerPåRiktigTidspunkt {
        @Test
        fun `tilOgMed = torsdag - velger fredag som er '1 uke tilbake'`() {
            val periode = Periode(fraOgMed = 10.november(2025), tilOgMed = 20.november(2025))

            val actual = periode.kanFyllesUtFraOgMed()

            actual shouldBe 14.november(2025).atTime(15, 0, 0)
        }

        @Test
        fun `tilOgMed = lørdag - velger fredagen som er før lørdagen`() {
            val periode = Periode(fraOgMed = 15.november(2025), tilOgMed = 22.november(2025))

            val actual = periode.kanFyllesUtFraOgMed()

            actual shouldBe 21.november(2025).atTime(15, 0, 0)
        }

        @Test
        fun `perioden inneholder ikke en fredag`() {
            val periode = Periode(fraOgMed = 19.november(2025), tilOgMed = 20.november(2025))

            assertThrows<IllegalArgumentException> {
                periode.kanFyllesUtFraOgMed()
            }
        }
    }
}
