package no.nav.tiltakspenger.routes.landingsside

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.objectmothers.lagMeldekort
import no.nav.tiltakspenger.routes.withTestApplicationContext
import org.junit.jupiter.api.Test

class LandingssideStatusRouteTest {

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
            val sak = ObjectMother.sak()
            tac.sakRepo.lagre(sak)

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
            val meldeperiode = ObjectMother.meldeperiode(periode = periode)

            val sak = ObjectMother.sak(meldeperioder = listOf(meldeperiode))
            tac.sakRepo.lagre(sak)

            val meldekort = tac.lagMeldekort(meldeperiode = meldeperiode, mottatt = null)

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

            val førsteMeldeperiode = ObjectMother.meldeperiode(periode = førstePeriode)
            val andreMeldeperiode = ObjectMother.meldeperiode(periode = andrePeriode)

            val sak = ObjectMother.sak(meldeperioder = listOf(førsteMeldeperiode, andreMeldeperiode))
            tac.sakRepo.lagre(sak)

            // Første meldekort er innsendt
            tac.lagMeldekort(
                meldeperiode = førsteMeldeperiode,
                mottatt = førstePeriode.tilOgMed.atStartOfDay(),
            )
            // Andre meldekort er klart men ikke innsendt
            val andreMeldekort = tac.lagMeldekort(meldeperiode = andreMeldeperiode, mottatt = null)

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = true,
                meldekortTilUtfylling = listOf(andreMeldekort.meldeperiode.kanFyllesUtFraOgMed),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }

    @Test
    fun `bruker med innsendte meldekort og ingen klare - returnerer harInnsendt uten meldekort til utfylling`() =
        runTest {
            withTestApplicationContext { tac ->
                val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
                val meldeperiode = ObjectMother.meldeperiode(periode = periode)

                val sak = ObjectMother.sak(meldeperioder = listOf(meldeperiode))
                tac.sakRepo.lagre(sak)

                tac.lagMeldekort(
                    meldeperiode = meldeperiode,
                    mottatt = periode.tilOgMed.atStartOfDay(),
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
    fun `flere meldekort klare til utfylling - returneres sortert etter kanSendesFra`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val førstePeriode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val andrePeriode = førstePeriode.plus14Dager()
            val tredjePeriode = andrePeriode.plus14Dager()

            val førsteMeldeperiode = ObjectMother.meldeperiode(periode = førstePeriode)
            val andreMeldeperiode = ObjectMother.meldeperiode(periode = andrePeriode)
            val tredjeMeldeperiode = ObjectMother.meldeperiode(periode = tredjePeriode)

            val sak = ObjectMother.sak(
                meldeperioder = listOf(førsteMeldeperiode, andreMeldeperiode, tredjeMeldeperiode),
            )
            tac.sakRepo.lagre(sak)

            // Lagrer meldekort i ulik rekkefølge for å verifisere at sorteringen skjer i servicelaget
            val tredjeMeldekort = tac.lagMeldekort(meldeperiode = tredjeMeldeperiode, mottatt = null)
            val førsteMeldekort = tac.lagMeldekort(meldeperiode = førsteMeldeperiode, mottatt = null)
            val andreMeldekort = tac.lagMeldekort(meldeperiode = andreMeldeperiode, mottatt = null)

            val body = landingssideStatusRequest()!!

            body.shouldBe(
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = listOf(
                    førsteMeldekort.meldeperiode.kanFyllesUtFraOgMed,
                    andreMeldekort.meldeperiode.kanFyllesUtFraOgMed,
                    tredjeMeldekort.meldeperiode.kanFyllesUtFraOgMed,
                ),
                redirectUrl = Configuration.meldekortFrontendUrl,
            )
        }
    }
}
