package no.nav.tiltakspenger.meldekort.landingsside.infra.routes

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.infra.Configuration
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContext
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.sendInnNesteMeldekort
import no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottaSakRequest
import no.nav.tiltakspenger.objectmothers.ObjectMother.arenaMeldekort
import no.nav.tiltakspenger.objectmothers.ObjectMother.arenaMeldekortOversikt
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import org.junit.jupiter.api.Test

class LandingssideStatusRouteTest {

    @Test
    fun `bruker uten sak - returnerer 404`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()

            landingssideStatusRequest(
                fnr = fnr.verdi,
                forventetStatus = HttpStatusCode.NotFound,
                forventetBody = "",
                forventetContentType = null,
            )
        }
    }

    @Test
    fun `bruker med sak men ingen meldekort - returnerer tom status`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()
            mottaSakRequest(tac = tac, fnr = fnr, meldeperioder = emptyList())

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!

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
            val fnr = tac.nesteFnr()
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

            mottaSakRequest(
                tac = tac,
                fnr = fnr,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )

            val meldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr).single()

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!

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
            val fnr = tac.nesteFnr()
            val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val andrePeriode = førstePeriode.plus14Dager()

            mottaSakRequest(
                tac = tac,
                fnr = fnr,
                meldeperioder = listOf(
                    meldeperiodeDto(periode = førstePeriode, opprettet = nå(tac.clock)),
                    meldeperiodeDto(periode = andrePeriode, opprettet = nå(tac.clock)),
                ),
            )

            // Sender inn det første meldekortet
            sendInnNesteMeldekort(tac = tac, fnr = fnr.verdi)!!

            val andreMeldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr).single()

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!

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
            val fnr = tac.nesteFnr()
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

            mottaSakRequest(
                tac = tac,
                fnr = fnr,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )

            sendInnNesteMeldekort(tac = tac, fnr = fnr.verdi)!!

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!

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
            val fnr = tac.nesteFnr()
            val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val andrePeriode = førstePeriode.plus14Dager()
            val tredjePeriode = andrePeriode.plus14Dager()

            // Sender inn meldeperiodene i ulik rekkefølge for å verifisere at sorteringen skjer i servicelaget
            mottaSakRequest(
                tac = tac,
                fnr = fnr,
                meldeperioder = listOf(
                    meldeperiodeDto(periode = tredjePeriode, opprettet = nå(tac.clock)),
                    meldeperiodeDto(periode = førstePeriode, opprettet = nå(tac.clock)),
                    meldeperiodeDto(periode = andrePeriode, opprettet = nå(tac.clock)),
                ),
            )

            val meldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr)
                .associateBy { it.meldeperiode.periode }

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!

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
            val fnr = tac.nesteFnr()
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

            mottaSakRequest(
                tac = tac,
                fnr = fnr,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )

            val meldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr).single()

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!
            val dto = body.meldekortTilUtfylling.single()

            val forventetKanSendesFra = meldekort.meldeperiode.kanFyllesUtFraOgMed
            dto.kanSendesFra shouldBe forventetKanSendesFra
            dto.kanFyllesUtFra shouldBe forventetKanSendesFra
            dto.fristForInnsending shouldBe null
        }
    }

    @Test
    fun `bruker med flere innsendte meldekort og ett klart - harInnsendteMeldekort er true`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val fnr = tac.nesteFnr()
            val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val andrePeriode = førstePeriode.plus14Dager()
            val tredjePeriode = andrePeriode.plus14Dager()

            mottaSakRequest(
                tac = tac,
                fnr = fnr,
                meldeperioder = listOf(
                    meldeperiodeDto(periode = førstePeriode, opprettet = nå(tac.clock)),
                    meldeperiodeDto(periode = andrePeriode, opprettet = nå(tac.clock)),
                    meldeperiodeDto(periode = tredjePeriode, opprettet = nå(tac.clock)),
                ),
            )

            // Sender inn de to første meldekortene
            sendInnNesteMeldekort(tac = tac, fnr = fnr.verdi)!!
            sendInnNesteMeldekort(tac = tac, fnr = fnr.verdi)!!

            val tredjeMeldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr).single()

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!

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
            val fnr = tac.nesteFnr()
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortOversikt(meldekortListe = listOf(arenaMeldekort(periode = periode))),
            )

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!

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
            val fnr = tac.nesteFnr()
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortOversikt(
                    meldekortListe =
                    listOf(arenaMeldekort(periode = periode, mottattDato = periode.tilOgMed)),
                ),
            )

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!

            body.shouldBe(
                harInnsendteMeldekort = true,
                meldekortTilUtfylling = emptyList(),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `feil ved henting av aktive arena-meldekort gir 404 når bruker ikke har sak`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val fnr = tac.nesteFnr()
            tac.arenaMeldekortClient.leggTilMeldekortFeil(fnr)

            landingssideStatusRequest(
                fnr = fnr.verdi,
                forventetStatus = HttpStatusCode.NotFound,
                forventetBody = "",
                forventetContentType = null,
            )
        }
    }

    @Test
    fun `feil ved henting av historiske arena-meldekort ignoreres`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val fnr = tac.nesteFnr()
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortOversikt(meldekortListe = listOf(arenaMeldekort(periode = periode))),
            )
            tac.arenaMeldekortClient.leggTilHistoriskMeldekortFeil(fnr)

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!

            body.shouldBe(
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = listOf(periode.tilOgMed.minusDays(1).atStartOfDay()),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `bruker med sak og arena-meldekort - kombineres og sorteres i respons`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val fnr = tac.nesteFnr()
            val arenaPeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val sakPeriode = arenaPeriode.plus14Dager()

            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortOversikt(meldekortListe = listOf(arenaMeldekort(periode = arenaPeriode))),
            )

            mottaSakRequest(
                tac = tac,
                fnr = fnr,
                meldeperioder = listOf(meldeperiodeDto(periode = sakPeriode, opprettet = nå(tac.clock))),
            )

            val sakMeldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr).single()

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!

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
            val fnr = tac.nesteFnr()
            val sakPeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val arenaPeriode = sakPeriode.plus14Dager()

            // Fake-en returnerer meldekort, men service skal ikke kalle arena siden saken markerer HAR_IKKE_MELDEKORT
            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortOversikt(meldekortListe = listOf(arenaMeldekort(periode = arenaPeriode))),
            )

            val sak = mottaSakRequest(
                tac = tac,
                fnr = fnr,
                meldeperioder = listOf(meldeperiodeDto(periode = sakPeriode, opprettet = nå(tac.clock))),
            )
            tac.sakRepo.oppdaterArenaStatus(sak.id, ArenaMeldekortStatus.HAR_IKKE_MELDEKORT)

            val sakMeldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr).single()

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!

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
            val fnr = tac.nesteFnr()
            val sakPeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val arenaPeriode = sakPeriode.plus14Dager()

            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortOversikt(
                    meldekortListe =
                    listOf(arenaMeldekort(periode = arenaPeriode, erTiltakspengerMeldekort = false)),
                ),
            )

            mottaSakRequest(
                tac = tac,
                fnr = fnr,
                meldeperioder = listOf(meldeperiodeDto(periode = sakPeriode, opprettet = nå(tac.clock))),
            )

            val sakMeldekort = tac.meldekortRepo.hentAlleMeldekortKlarTilInnsending(fnr).single()

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!

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
            val fnr = tac.nesteFnr()
            val aktivPeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val historiskPeriode = Periode(fraOgMed = 9.desember(2024), tilOgMed = 22.desember(2024))

            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortOversikt(meldekortListe = listOf(arenaMeldekort(periode = aktivPeriode))),
            )
            tac.arenaMeldekortClient.leggTilHistoriskMeldekort(
                fnr,
                arenaMeldekortOversikt(
                    meldekortListe =
                    listOf(arenaMeldekort(periode = historiskPeriode, mottattDato = historiskPeriode.tilOgMed)),
                ),
            )

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!

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
            val fnr = tac.nesteFnr()
            val aktivPeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val historiskPeriode = Periode(fraOgMed = 9.desember(2024), tilOgMed = 22.desember(2024))

            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortOversikt(meldekortListe = listOf(arenaMeldekort(periode = aktivPeriode))),
            )
            tac.arenaMeldekortClient.leggTilHistoriskMeldekort(
                fnr,
                arenaMeldekortOversikt(
                    meldekortListe =
                    listOf(
                        arenaMeldekort(
                            periode = historiskPeriode,
                            mottattDato = historiskPeriode.tilOgMed,
                            erTiltakspengerMeldekort = false,
                        ),
                    ),
                ),
            )

            val body = landingssideStatusRequest(fnr = fnr.verdi)!!

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
            val fnr = tac.nesteFnr()
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortOversikt(
                    meldekortListe =
                    listOf(arenaMeldekort(periode = periode, erTiltakspengerMeldekort = false)),
                ),
            )

            landingssideStatusRequest(
                fnr = fnr.verdi,
                forventetStatus = HttpStatusCode.NotFound,
                forventetBody = "",
                forventetContentType = null,
            )
        }
    }
}
