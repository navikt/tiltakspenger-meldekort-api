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
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.tilSak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.lagreMeldekortFraBrukerKommando
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MottaSakerRouteTest {
    private suspend fun ApplicationTestBuilder.mottaSakRequest(dto: SakTilMeldekortApiDTO) = defaultRequest(
        HttpMethod.Post,
        url {
            protocol = URLProtocol.HTTPS
            path("/saksbehandling/sak")
        },
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

                tac.meldekortRepo.hentAlleMeldekortForBruker(sak.fnr).size shouldBe 2
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
    fun `Skal opprette nytt meldekort og ikke deaktivere meldekort som er mottatt`() {
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

                val meldekort = tac.meldekortRepo.hentMeldekortForKjedeId(
                    MeldeperiodeKjedeId(meldeperiode.kjedeId),
                    Fnr.fromString(sakDto1.fnr),
                ).first()

                val meldeperiode = sakDto1.tilSak().meldeperioder.first()

                val lagreKommando = lagreMeldekortFraBrukerKommando(
                    meldeperiode = meldeperiode,
                    meldekortId = meldekort.id,
                )

                tac.meldekortRepo.lagreFraBruker(lagreKommando)
            }

            mottaSakRequest(sakDto2).apply {
                status shouldBe HttpStatusCode.OK

                val (førsteMeldekort, andreMeldekort) = tac.meldekortRepo.hentMeldekortForKjedeId(
                    MeldeperiodeKjedeId(meldeperiode.kjedeId),
                    Fnr.fromString(sakDto1.fnr),
                )

                førsteMeldekort.deaktivert shouldBe null

                andreMeldekort.deaktivert shouldBe null
                førsteMeldekort.varselId shouldBe andreMeldekort.varselId
            }
        }
    }
}
