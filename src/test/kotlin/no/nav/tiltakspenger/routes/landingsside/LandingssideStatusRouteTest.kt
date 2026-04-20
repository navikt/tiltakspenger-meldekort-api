package no.nav.tiltakspenger.routes.landingsside

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.meldekort.clients.arena.ArenaMeldekort
import no.nav.tiltakspenger.meldekort.clients.arena.ArenaMeldekortResponse
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside.LandingssideStatusDTO.LandingssideMeldekortDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.routes.saksbehandling.mottaSakRequest
import no.nav.tiltakspenger.routes.sendinnmeldekort.sendInnNesteMeldekort
import no.nav.tiltakspenger.routes.withTestApplicationContext
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.random.Random

class LandingssideStatusRouteTest {

    private val fnr = Fnr.fromString(FAKE_FNR)

    @Test
    fun `bruker uten sak - returnerer 404`() = runTest {
        withTestApplicationContext { _ ->
            landingssideStatusRequest(
                forventetStatus = HttpStatusCode.NotFound,
                forventetBody = "",
                forventetContentType = null,
            )
        }
    }

    @Test
    fun `bruker med sak men ingen meldekort - returnerer tom status`() = runTest {
        withTestApplicationContext { tac ->
            mottaSakRequest(tac = tac, meldeperioder = emptyList())

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = emptyList(),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `bruker med klart meldekort men ingen innsendt - returnerer meldekort til utfylling`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )

            val meldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr).single()

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = listOf(
                    meldekort.meldeperiode.kanFyllesUtFraOgMed,
                ),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `bruker med ett innsendt meldekort og ett klart til utfylling`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val andrePeriode = førstePeriode.plus14Dager()

            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(
                    meldeperiodeDto(periode = førstePeriode, opprettet = nå(tac.clock)),
                    meldeperiodeDto(periode = andrePeriode, opprettet = nå(tac.clock)),
                ),
            )

            // Sender inn det første meldekortet
            sendInnNesteMeldekort(tac = tac)!!

            val andreMeldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr).single()

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = true,
                meldekortTilUtfylling = listOf(andreMeldekort.meldeperiode.kanFyllesUtFraOgMed),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `bruker med innsendte meldekort og ingen klare - returnerer harInnsendt uten meldekort til utfylling`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )

            sendInnNesteMeldekort(tac = tac)!!

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = true,
                meldekortTilUtfylling = emptyList(),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `flere meldekort klare til utfylling - returneres sortert etter kanSendesFra`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val andrePeriode = førstePeriode.plus14Dager()
            val tredjePeriode = andrePeriode.plus14Dager()

            // Sender inn meldeperiodene i ulik rekkefølge for å verifisere at sorteringen skjer i servicelaget
            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(
                    meldeperiodeDto(periode = tredjePeriode, opprettet = nå(tac.clock)),
                    meldeperiodeDto(periode = førstePeriode, opprettet = nå(tac.clock)),
                    meldeperiodeDto(periode = andrePeriode, opprettet = nå(tac.clock)),
                ),
            )

            val meldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr)
                .associateBy { it.meldeperiode.periode }

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = listOf(
                    meldekort.getValue(førstePeriode).meldeperiode.kanFyllesUtFraOgMed,
                    meldekort.getValue(andrePeriode).meldeperiode.kanFyllesUtFraOgMed,
                    meldekort.getValue(tredjePeriode).meldeperiode.kanFyllesUtFraOgMed,
                ),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `kanFyllesUtFra er lik kanSendesFra og fristForInnsending er null`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )

            val meldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr).single()

            val body = landingssideStatusRequest()!!
            val dto = body.meldekortTilUtfylling.single()

            val forventet = LandingssideMeldekortDTO(kanSendesFra = meldekort.meldeperiode.kanFyllesUtFraOgMed)
            dto.kanSendesFra shouldBe forventet.kanSendesFra
            dto.kanFyllesUtFra shouldBe forventet.kanSendesFra
            dto.fristForInnsending shouldBe null
        }
    }

    @Test
    fun `bruker med flere innsendte meldekort og ett klart - harInnsendteMeldekort er true`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val andrePeriode = førstePeriode.plus14Dager()
            val tredjePeriode = andrePeriode.plus14Dager()

            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(
                    meldeperiodeDto(periode = førstePeriode, opprettet = nå(tac.clock)),
                    meldeperiodeDto(periode = andrePeriode, opprettet = nå(tac.clock)),
                    meldeperiodeDto(periode = tredjePeriode, opprettet = nå(tac.clock)),
                ),
            )

            // Sender inn de to første meldekortene
            sendInnNesteMeldekort(tac = tac)!!
            sendInnNesteMeldekort(tac = tac)!!

            val tredjeMeldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr).single()

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = true,
                meldekortTilUtfylling = listOf(tredjeMeldekort.meldeperiode.kanFyllesUtFraOgMed),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `bruker uten sak men med uinnsendt arena-meldekort - returnerer arena-meldekort`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortResponse(listOf(arenaMeldekort(periode = periode))),
            )

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = listOf(periode.tilOgMed.minusDays(1).atStartOfDay()),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `bruker uten sak men med innsendt arena-meldekort - harInnsendteMeldekort er true`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortResponse(
                    listOf(arenaMeldekort(periode = periode, mottattDato = periode.tilOgMed)),
                ),
            )

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = true,
                meldekortTilUtfylling = emptyList(),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `bruker med sak og arena-meldekort - kombineres og sorteres i respons`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val arenaPeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val sakPeriode = arenaPeriode.plus14Dager()

            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortResponse(listOf(arenaMeldekort(periode = arenaPeriode))),
            )

            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = sakPeriode, opprettet = nå(tac.clock))),
            )

            val sakMeldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr).single()

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = listOf(
                    arenaPeriode.tilOgMed.minusDays(1).atStartOfDay(),
                    sakMeldekort.meldeperiode.kanFyllesUtFraOgMed,
                ),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `arena-meldekort ignoreres når sak er markert som HAR_IKKE_MELDEKORT i arena`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val sakPeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val arenaPeriode = sakPeriode.plus14Dager()

            // Fake-en returnerer meldekort, men service skal ikke kalle arena siden saken markerer HAR_IKKE_MELDEKORT
            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortResponse(listOf(arenaMeldekort(periode = arenaPeriode))),
            )

            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = sakPeriode, opprettet = nå(tac.clock))),
            )
            tac.sakRepo.oppdaterArenaStatus(sak.id, ArenaMeldekortStatus.HAR_IKKE_MELDEKORT)

            val sakMeldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr).single()

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = listOf(sakMeldekort.meldeperiode.kanFyllesUtFraOgMed),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `arena-meldekort som ikke er tiltakspenger-meldekort inkluderes ikke i responsen`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val sakPeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val arenaPeriode = sakPeriode.plus14Dager()

            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortResponse(
                    listOf(arenaMeldekort(periode = arenaPeriode, erTiltakspengerMeldekort = false)),
                ),
            )

            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = sakPeriode, opprettet = nå(tac.clock))),
            )

            val sakMeldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr).single()

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = listOf(sakMeldekort.meldeperiode.kanFyllesUtFraOgMed),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `historisk innsendt arena-meldekort gir harInnsendteMeldekort true selv om aktive ikke er innsendt`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val aktivPeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val historiskPeriode = Periode(fraOgMed = 9.desember(2024), tilOgMed = 22.desember(2024))

            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortResponse(listOf(arenaMeldekort(periode = aktivPeriode))),
            )
            tac.arenaMeldekortClient.leggTilHistoriskMeldekort(
                fnr,
                arenaMeldekortResponse(
                    listOf(arenaMeldekort(periode = historiskPeriode, mottattDato = historiskPeriode.tilOgMed)),
                ),
            )

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = true,
                meldekortTilUtfylling = listOf(aktivPeriode.tilOgMed.minusDays(1).atStartOfDay()),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `historiske meldekort som ikke er tiltakspenger-meldekort påvirker ikke responsen`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val aktivPeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val historiskPeriode = Periode(fraOgMed = 9.desember(2024), tilOgMed = 22.desember(2024))

            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortResponse(listOf(arenaMeldekort(periode = aktivPeriode))),
            )
            tac.arenaMeldekortClient.leggTilHistoriskMeldekort(
                fnr,
                arenaMeldekortResponse(
                    listOf(
                        arenaMeldekort(
                            periode = historiskPeriode,
                            mottattDato = historiskPeriode.tilOgMed,
                            erTiltakspengerMeldekort = false,
                        ),
                    ),
                ),
            )

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = listOf(aktivPeriode.tilOgMed.minusDays(1).atStartOfDay()),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `bruker uten sak og kun ikke-tiltakspenger-meldekort i arena - returnerer 404`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortResponse(
                    listOf(arenaMeldekort(periode = periode, erTiltakspengerMeldekort = false)),
                ),
            )

            landingssideStatusRequest(
                forventetStatus = HttpStatusCode.NotFound,
                forventetBody = "",
                forventetContentType = null,
            )
        }
    }

    private fun arenaMeldekort(
        periode: Periode,
        mottattDato: LocalDate? = null,
        erTiltakspengerMeldekort: Boolean = true,
    ): ArenaMeldekort = ArenaMeldekort(
        meldekortId = Random.nextLong(),
        kortType = "",
        meldeperiode = "${periode.fraOgMed}-${periode.tilOgMed}",
        fraDato = periode.fraOgMed,
        tilDato = periode.tilOgMed,
        hoyesteMeldegruppe = if (erTiltakspengerMeldekort) "INDIV" else "ARBS",
        beregningstatus = "",
        forskudd = false,
        mottattDato = mottattDato,
    )

    private fun arenaMeldekortResponse(meldekort: List<ArenaMeldekort>): ArenaMeldekortResponse =
        ArenaMeldekortResponse(
            personId = 1L,
            etternavn = "Testesen",
            fornavn = "Test",
            maalformkode = "",
            meldeform = "",
            meldekortListe = meldekort,
        )
}
