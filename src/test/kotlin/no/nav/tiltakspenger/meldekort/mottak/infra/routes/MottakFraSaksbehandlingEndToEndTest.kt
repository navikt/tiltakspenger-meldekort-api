package no.nav.tiltakspenger.meldekort.mottak.infra.routes

import io.github.oshai.kotlinlogging.Level
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.lagreSak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContext
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.meldekort.mottak.infra.tilMottattSak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.tilMottattSak
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Ende-til-ende-tester for mottak av sak fra saksbehandling-api: kjører ekte requests inn på [no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottakFraSaksbehandlingRoute] og verifiserer både HTTP-svar (status/body) og sideeffekter (lagret sak, meldeperioder, meldekort, meldekortvedtak).
 *
 * Vi mocker ikke servicen for å treffe `Left`-grenene i route + service; vi fremprovoserer dem med ekte/fake database i stedet (duplikat → [no.nav.tiltakspenger.meldekort.mottak.FeilVedMottakAvSak.FinnesUtenDiff], diff på meldeperiode → MeldeperiodeFinnesMedDiff, databasekonstraint-brudd → LagringFeilet).
 */
class MottakFraSaksbehandlingEndToEndTest {

    private val førstePeriode = Periode(
        fraOgMed = LocalDate.of(2025, 1, 6),
        tilOgMed = LocalDate.of(2025, 1, 19),
    )

    @Test
    fun `Skal lagre saken og opprette meldekort`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val sakDto = ObjectMother.sakDTO(
                meldeperioder = listOf(
                    ObjectMother.meldeperiodeDto(periode = førstePeriode, opprettet = nå(tac.clock)),
                    ObjectMother.meldeperiodeDto(periode = førstePeriode.plus14Dager(), opprettet = nå(tac.clock)),
                ),
            )

            val id = SakId.fromString(sakDto.sakId)

            mottaSakRequest(
                tac = tac,
                requestDto = sakDto,
                forventet = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
            ).apply {
                val sak = tac.sakRepo.hent(id)

                sak!!.tilMottattSak() shouldBe sakDto.tilMottattSak()
                sak.meldeperioder.size shouldBe 2
                sak.harSoknadUnderBehandling shouldBe false

                tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(sak.fnr).size shouldBe 2
            }
        }
    }

    @Test
    fun `Skal lagre sak med meldeperiode og meldekortvedtak`() = runTest {
        withTestApplicationContext { tac ->
            val meldeperiode = ObjectMother.meldeperiodeDto(periode = førstePeriode, opprettet = nå(tac.clock))
            val vedtak = ObjectMother.meldekortvedtakDTO(meldeperiode = meldeperiode, opprettet = nå(tac.clock))
            val sakDto = ObjectMother.sakDTO(
                meldeperioder = listOf(meldeperiode),
                meldekortvedtak = listOf(vedtak),
            )

            mottaSakRequest(
                tac = tac,
                requestDto = sakDto,
                forventet = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
            ).apply {
                val sak = tac.sakRepo.hent(SakId.fromString(sakDto.sakId))!!

                sak.meldekortvedtakIdListe.map { it.toString() } shouldBe listOf(vedtak.id)
                sak.meldeperioder.map { it.id.toString() } shouldBe listOf(meldeperiode.id)
            }
        }
    }

    @Test
    fun `Skal oppdatere sak hvis harSoknadUnderBehandling endres`() = runTest {
        withTestApplicationContext { tac ->
            val lagretSak = ObjectMother.sak(harSoknadUnderBehandling = false)
            tac.lagreSak(lagretSak)

            val id = lagretSak.id
            val sakDto = ObjectMother.sakDTO(
                sakId = id.toString(),
                saksnummer = lagretSak.saksnummer,
                fnr = lagretSak.fnr.verdi,
                meldeperioder = emptyList(),
                harSoknadUnderBehandling = true,
            )

            mottaSakRequest(
                tac = tac,
                requestDto = sakDto,
                forventet = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
            ).apply {
                val sak = tac.sakRepo.hent(id)

                sak!!.tilMottattSak() shouldBe sakDto.tilMottattSak()
                sak.meldeperioder.size shouldBe 0
                sak.harSoknadUnderBehandling shouldBe true
            }
        }
    }

    @Test
    fun `Skal håndtere duplikate requests for lagring av sak og returnere ok`() = runTest {
        withTestApplicationContext { tac ->
            val sakDto = ObjectMother.sakDTO(
                meldeperioder = listOf(
                    ObjectMother.meldeperiodeDto(periode = førstePeriode, opprettet = nå(tac.clock)),
                    ObjectMother.meldeperiodeDto(periode = førstePeriode.plus14Dager(), opprettet = nå(tac.clock)),
                ),
            )

            mottaSakRequest(
                tac = tac,
                requestDto = sakDto,
                forventet = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
            )

            mottaSakRequest(
                tac = tac,
                requestDto = sakDto,
                forventet = ForventetRespons.eksakt(200, "Saken var allerede lagret med samme data", "text/plain; charset=UTF-8"),
            )
            tac.sakRepo.hent(SakId.fromString(sakDto.sakId))!!.tilMottattSak() shouldBe sakDto.tilMottattSak()
        }
    }

    @Test
    fun `Skal håndtere oppdatering av sak med nye meldeperioder`() = runTest {
        withTestApplicationContext { tac ->
            val førsteMeldeperiode = ObjectMother.meldeperiodeDto(
                periode = førstePeriode,
                opprettet = nå(tac.clock),
            )

            val andreMeldeperiode = ObjectMother.meldeperiodeDto(
                periode = førstePeriode.plus14Dager(),
                opprettet = nå(tac.clock),
            )

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

            mottaSakRequest(
                tac = tac,
                requestDto = sakDto1,
                forventet = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
            )

            mottaSakRequest(
                tac = tac,
                requestDto = sakDto2,
                forventet = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
            ).apply {
                val sak = tac.sakRepo.hent(SakId.fromString(sakDto2.sakId))!!

                sak.meldeperioder.size shouldBe 2
            }
        }
    }

    @Test
    fun `Skal returnere 409 ved lagring av sak med ulike meldeperioder med samme meldeperiode-id`() = runTest {
        withTestApplicationContext { tac ->
            val førsteMeldeperiode = ObjectMother.meldeperiodeDto(
                periode = førstePeriode,
                opprettet = nå(tac.clock),
            )

            val andreMeldeperiode = ObjectMother.meldeperiodeDto(
                periode = førstePeriode.plus14Dager(),
                opprettet = nå(tac.clock),
            )

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

            mottaSakRequest(
                tac = tac,
                requestDto = sakDto1,
                forventet = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
            )

            mottaSakRequest(
                tac = tac,
                requestDto = sakDto2,
                forventet = ForventetRespons.eksakt(409, "Meldeperiode var allerede lagret med andre data", "text/plain; charset=UTF-8"),
            )

            val meldeperiode = tac.meldeperiodeRepo.hentForId(MeldeperiodeId.fromString(førsteMeldeperiode.id))!!
            meldeperiode.opprettet shouldBe førsteMeldeperiode.opprettet
        }
    }

    @Test
    fun `Skal opprette nytt meldekort og deaktivere forrige ved ny meldeperiode-versjon`() = runTest {
        withTestApplicationContext { tac ->
            val meldeperiode = ObjectMother.meldeperiodeDto(
                periode = førstePeriode,
                opprettet = nå(tac.clock),
            )
            val nyMeldeperiodeVersjon = ObjectMother.meldeperiodeDto(
                periode = førstePeriode,
                versjon = 2,
                opprettet = nå(tac.clock),
            )

            val sakDto1 = ObjectMother.sakDTO(
                meldeperioder = listOf(meldeperiode),
            )
            val sakDto2 = sakDto1.copy(
                meldeperioder = listOf(nyMeldeperiodeVersjon),
            )

            mottaSakRequest(
                tac = tac,
                requestDto = sakDto1,
                forventet = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
            )

            mottaSakRequest(
                tac = tac,
                requestDto = sakDto2,
                forventet = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
            ).apply {
                val (førsteMeldekort, andreMeldekort) =
                    tac.meldekortRepo.hentMeldekortForKjedeId(
                        MeldeperiodeKjedeId(meldeperiode.kjedeId),
                        Fnr.fromString(sakDto1.fnr),
                    )

                førsteMeldekort.deaktivert shouldNotBe null

                andreMeldekort.deaktivert shouldBe null
            }
        }
    }

    @Test
    fun `Skal ikke opprette nytt meldekort for kjede der meldekort allerede er mottatt`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val meldeperiode = ObjectMother.meldeperiodeDto(
                periode = førstePeriode,
                opprettet = nå(tac.clock),
            )
            val nyMeldeperiodeVersjon = ObjectMother.meldeperiodeDto(
                periode = førstePeriode,
                versjon = 2,
                opprettet = nå(tac.clock),
            )

            val sakDto1 = ObjectMother.sakDTO(
                meldeperioder = listOf(meldeperiode),
            )
            val sakDto2 = sakDto1.copy(
                meldeperioder = listOf(nyMeldeperiodeVersjon),
            )

            mottaSakRequest(
                tac = tac,
                requestDto = sakDto1,
                forventet = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
            ).apply {
                val meldekort = tac.meldekortRepo.hentMeldekortForKjedeId(
                    MeldeperiodeKjedeId(meldeperiode.kjedeId),
                    Fnr.fromString(sakDto1.fnr),
                ).first()

                val meldeperiode = meldekort.meldeperiode

                val lagreKommando = ObjectMother.lagreMeldekortFraBrukerKommando(
                    meldeperiode = meldeperiode,
                    meldekortId = meldekort.id,
                )

                tac.meldekortRepo.lagre(
                    meldekort.fyllUtMeldekortFraBruker(
                        sisteMeldeperiode = meldeperiode,
                        clock = tac.clock,
                        brukerutfylteDager = lagreKommando.dager,
                        korrigering = false,
                        locale = null,
                    ),
                )
            }

            mottaSakRequest(
                tac = tac,
                requestDto = sakDto2,
                forventet = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
            ).apply {
                val meldekortFraKjede = tac.meldekortRepo.hentMeldekortForKjedeId(
                    MeldeperiodeKjedeId(meldeperiode.kjedeId),
                    Fnr.fromString(sakDto1.fnr),
                )

                meldekortFraKjede.size shouldBe 1
            }
        }
    }

    @Test
    fun `Skal deaktivere gammelt meldekort uten nytt varselgrunnlag når revurdering fjerner all rett`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val periode = Periode(
                fraOgMed = 6.januar(2025),
                tilOgMed = 19.januar(2025),
            )
            val meldeperiode = ObjectMother.meldeperiodeDto(
                periode = periode,
                opprettet = nå(tac.clock),
            )
            val sakDto = ObjectMother.sakDTO(meldeperioder = listOf(meldeperiode))

            val sak = mottaSakRequest(
                tac = tac,
                requestDto = sakDto,
                forventet = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
            )
            val opprinneligMeldekort = tac.hentMeldekortService.hentNesteMeldekortForUtfylling(sak.fnr)!!

            val sakDtoOppdater = sakDto.copy(
                meldeperioder = listOf(
                    meldeperiode.copy(
                        id = MeldeperiodeId.random().toString(),
                        versjon = 2,
                        girRett = periode.tilDager().associateWith { false },
                        antallDagerForPeriode = 0,
                    ),
                ),
            )

            mottaSakRequest(
                tac = tac,
                requestDto = sakDtoOppdater,
                forventet = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
            ).apply {
                tac.hentMeldekortService.hentNesteMeldekortForUtfylling(sak.fnr).shouldBeNull()

                val meldekortForKjede = tac.meldekortRepo.hentMeldekortForKjedeId(opprinneligMeldekort.meldeperiode.kjedeId, sak.fnr)
                meldekortForKjede.size shouldBe 1
                meldekortForKjede.single().also { deaktivertMeldekort ->
                    deaktivertMeldekort.id shouldBe opprinneligMeldekort.id
                    deaktivertMeldekort.deaktivert shouldNotBe null
                }
            }
        }
    }

    @Test
    fun `Skal returnere 400 ved ugyldig JSON-body`() = runTest {
        withTestApplicationContext { _ ->
            defaultRequestWithAssertions(
                method = HttpMethod.POST,
                uri = "/saksbehandling/sak",
                jwt = JwtGenerator().createJwtForSystembruker(),
                forventet = ForventetRespons(
                    status = 400,
                    body = ForventetBody.Eksakt("Feil ved parsing av sak fra saksbehandling-api"),
                    contentType = "text/plain; charset=UTF-8",
                ),
                body = "{ ikke gyldig json",
            )
        }
    }

    @Test
    fun `Skal returnere 400 dersom DTO ikke kan parses til en gyldig sakId`() = runTest {
        withTestApplicationContext { _ ->
            // SakId.fromString på ugyldig id kaster IllegalArgumentException → 400 (klientfeil), ikke 500. Slik unngår avsender unødvendige retries og alarmer.
            val ugyldigDto = ObjectMother.sakDTO(sakId = "ikke-en-gyldig-sakid")
            defaultRequestWithAssertions(
                method = HttpMethod.POST,
                uri = "/saksbehandling/sak",
                jwt = JwtGenerator().createJwtForSystembruker(),
                forventet = ForventetRespons(
                    status = 400,
                    body = ForventetBody.Eksakt("Feil ved mapping av sak-DTO til domenemodell under mottak av sak fra saksbehandling-api. sakId: ikke-en-gyldig-sakid. Antall meldeperioder: 0. Antall meldekortvedtak: 0."),
                    contentType = "text/plain; charset=UTF-8",
                ),
                body = serialize(ugyldigDto),
            )
        }
    }

    @Test
    fun `Skal returnere 400 ved payload som bryter domeneinvariant`() = runTest {
        withTestApplicationContext { tac ->
            // To meldeperioder med samme periode og samme versjon bryter Sak.init sin require(a.periode.tilOgMed < b.periode.fraOgMed || (a.periode == b.periode && a.versjon < b.versjon)).
            // Skal gi 400 (klientfeil), ikke 500 (som ville trigget retries og alarmer hos avsender).
            val periode = ObjectMother.meldeperiodeDto(periode = førstePeriode, opprettet = nå(tac.clock))
            val ugyldigSakDto = ObjectMother.sakDTO(
                meldeperioder = listOf(periode, periode.copy(id = MeldeperiodeId.random().toString())),
            )

            defaultRequestWithAssertions(
                method = HttpMethod.POST,
                uri = "/saksbehandling/sak",
                jwt = JwtGenerator().createJwtForSystembruker(),
                forventet = ForventetRespons(
                    status = 400,
                    body = ForventetBody.Eksakt("Feil ved mapping av sak-DTO til domenemodell under mottak av sak fra saksbehandling-api. sakId: ${ugyldigSakDto.sakId}. Antall meldeperioder: 2. Antall meldekortvedtak: 0."),
                    contentType = "text/plain; charset=UTF-8",
                ),
                body = serialize(ugyldigSakDto),
            )

            tac.sakRepo.hent(SakId.fromString(ugyldigSakDto.sakId)) shouldBe null
        }
    }

    /**
     * Treffer den defensive `LagringFeilet`-grenen (HTTP 500) via en genuin databasefeil mot ekte Postgres: en ny meldeperiode-id med samme `(sak_id, kjede_id, opprettet)` som en allerede lagret meldeperiode bryter `unique_kjede_id_opprettet` (se V7-migreringen).
     * `versjon` bumpes, slik at det kun er denne constrainten som brytes.
     *
     * Vi asserter på loggeren i tillegg til HTTP-svaret: uten det ville testen ikke merket om det var en helt annen databasefeil som ga 500-en.
     * Se søsterstesten under for `unique_kjede_id_versjon`.
     */
    @Test
    fun `Skal returnere 500 og rulle tilbake når lagring bryter unique_kjede_id_opprettet`() {
        withTestApplicationContextAndPostgres { tac ->
            val meldeperiode = ObjectMother.meldeperiodeDto(periode = førstePeriode, opprettet = nå(tac.clock))

            kolliderMedLagretMeldeperiode(
                tac = tac,
                meldeperiode = meldeperiode,
                kolliderende = meldeperiode.copy(
                    id = MeldeperiodeId.random().toString(),
                    versjon = meldeperiode.versjon + 1,
                ),
                forventetConstraint = "unique_kjede_id_opprettet",
            )
        }
    }

    /**
     * Som testen over, men her holdes `versjon` fast og `opprettet` flyttes, slik at det kun er `unique_kjede_id_versjon` som brytes.
     */
    @Test
    fun `Skal returnere 500 og rulle tilbake når lagring bryter unique_kjede_id_versjon`() {
        withTestApplicationContextAndPostgres { tac ->
            val meldeperiode = ObjectMother.meldeperiodeDto(periode = førstePeriode, opprettet = nå(tac.clock))

            kolliderMedLagretMeldeperiode(
                tac = tac,
                meldeperiode = meldeperiode,
                kolliderende = meldeperiode.copy(
                    id = MeldeperiodeId.random().toString(),
                    opprettet = meldeperiode.opprettet.plusSeconds(1),
                ),
                forventetConstraint = "unique_kjede_id_versjon",
            )
        }
    }

    /**
     * Lagrer [meldeperiode], sender så inn [kolliderende] på samme sak og verifiserer at forsøket gir HTTP 500, at [forventetConstraint] er det som faktisk brøt, og at hele transaksjonen ble rullet tilbake.
     */
    private suspend fun ApplicationTestBuilder.kolliderMedLagretMeldeperiode(
        tac: TestApplicationContext,
        meldeperiode: SakTilMeldekortApiDTO.MeldeperiodeDTO,
        kolliderende: SakTilMeldekortApiDTO.MeldeperiodeDTO,
        forventetConstraint: String,
    ) {
        val sakDto = ObjectMother.sakDTO(meldeperioder = listOf(meldeperiode))

        mottaSakRequest(
            tac = tac,
            requestDto = sakDto,
            forventet = ForventetRespons.eksakt(200, "Sak lagret", "text/plain; charset=UTF-8"),
        )

        mottaSakRequest(
            tac = tac,
            requestDto = sakDto.copy(meldeperioder = listOf(kolliderende)),
            forventet = ForventetRespons.eksakt(500, "Lagring av sak feilet", "text/plain; charset=UTF-8"),
        )

        tac.mottakLoggfanger.linjerPå(Level.ERROR).single().also {
            it.melding shouldContain "Feil under lagring av sak eller meldeperioder for ${sakDto.sakId}"
            it.årsakskjede() shouldContain "Feil under lagring av meldeperiode ${kolliderende.id}"
            it.årsakskjede() shouldContain forventetConstraint
        }

        // Saken skal fortsatt bare ha den opprinnelige meldeperioden (transaksjonen rullet tilbake).
        val lagretSak = tac.sakRepo.hent(SakId.fromString(sakDto.sakId))!!
        lagretSak.meldeperioder.map { it.id.toString() } shouldBe listOf(meldeperiode.id)
    }
}
