package no.nav.tiltakspenger.routes

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.Configuration
import no.nav.tiltakspenger.meldekort.routes.meldekort.landingsside.LandingssideStatusDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.lagMeldekort
import org.junit.jupiter.api.Test

class LandingssideStatusRouteTest {

    private suspend fun ApplicationTestBuilder.landingssideStatusRequest() = defaultRequest(
        HttpMethod.Get,
        url {
            protocol = URLProtocol.HTTPS
            path("/landingsside/status")
        },
        jwt = JwtGenerator().createJwtForUser(),
    )

    @Test
    fun `bruker uten sak - returnerer 404`() {
        val tac = TestApplicationContext()

        testMedMeldekortRoutes(tac) {
            landingssideStatusRequest().apply {
                status shouldBe HttpStatusCode.NotFound
            }
        }
    }

    @Test
    fun `bruker med sak men ingen meldekort - returnerer tom status`() {
        val tac = TestApplicationContext()

        val sak = ObjectMother.sak()
        tac.sakRepo.lagre(sak)

        testMedMeldekortRoutes(tac) {
            landingssideStatusRequest().apply {
                status shouldBe HttpStatusCode.OK

                val body = deserialize<LandingssideStatusDTO>(bodyAsText())

                body.harInnsendteMeldekort shouldBe false
                body.meldekortTilUtfylling shouldHaveSize 0
                body.redirectUrl shouldBe Configuration.meldekortFrontendUrl
            }
        }
    }

    @Test
    fun `bruker med klart meldekort men ingen innsendt - returnerer meldekort til utfylling`() {
        val tac = TestApplicationContext()
        (tac.clock as TikkendeKlokke).spolTil(1.mars(2025))

        val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
        val meldeperiode = ObjectMother.meldeperiode(periode = periode)

        val sak = ObjectMother.sak(meldeperioder = listOf(meldeperiode))
        tac.sakRepo.lagre(sak)

        val meldekort = tac.lagMeldekort(meldeperiode = meldeperiode, mottatt = null)

        testMedMeldekortRoutes(tac) {
            landingssideStatusRequest().apply {
                status shouldBe HttpStatusCode.OK

                val body = deserialize<LandingssideStatusDTO>(bodyAsText())

                body.harInnsendteMeldekort shouldBe false
                body.meldekortTilUtfylling shouldHaveSize 1
                body.meldekortTilUtfylling.first().kanSendesFra shouldBe meldekort.meldeperiode.kanFyllesUtFraOgMed
                body.meldekortTilUtfylling.first().kanFyllesUtFra shouldBe meldekort.meldeperiode.kanFyllesUtFraOgMed
                body.meldekortTilUtfylling.first().fristForInnsending shouldBe meldekort.meldeperiode.kanFyllesUtFraOgMed.plusYears(10)
                body.redirectUrl shouldBe Configuration.meldekortFrontendUrl
            }
        }
    }

    @Test
    fun `bruker med ett innsendt meldekort og ett klart til utfylling`() {
        val tac = TestApplicationContext()
        (tac.clock as TikkendeKlokke).spolTil(1.mars(2025))

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
        tac.lagMeldekort(meldeperiode = andreMeldeperiode, mottatt = null)

        testMedMeldekortRoutes(tac) {
            landingssideStatusRequest().apply {
                status shouldBe HttpStatusCode.OK

                val body = deserialize<LandingssideStatusDTO>(bodyAsText())

                body.harInnsendteMeldekort shouldBe true
                body.meldekortTilUtfylling shouldHaveSize 1
                body.redirectUrl shouldBe Configuration.meldekortFrontendUrl
            }
        }
    }

    @Test
    fun `bruker med innsendte meldekort og ingen klare - returnerer harInnsendt uten meldekort til utfylling`() {
        val tac = TestApplicationContext()
        (tac.clock as TikkendeKlokke).spolTil(1.mars(2025))

        val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
        val meldeperiode = ObjectMother.meldeperiode(periode = periode)

        val sak = ObjectMother.sak(meldeperioder = listOf(meldeperiode))
        tac.sakRepo.lagre(sak)

        tac.lagMeldekort(
            meldeperiode = meldeperiode,
            mottatt = periode.tilOgMed.atStartOfDay(),
        )

        testMedMeldekortRoutes(tac) {
            landingssideStatusRequest().apply {
                status shouldBe HttpStatusCode.OK

                val body = deserialize<LandingssideStatusDTO>(bodyAsText())

                body.harInnsendteMeldekort shouldBe true
                body.meldekortTilUtfylling shouldHaveSize 0
                body.redirectUrl shouldBe Configuration.meldekortFrontendUrl
            }
        }
    }

    @Test
    fun `flere meldekort klare til utfylling - returneres sortert etter kanSendesFra`() {
        val tac = TestApplicationContext()
        (tac.clock as TikkendeKlokke).spolTil(1.mars(2025))

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

        testMedMeldekortRoutes(tac) {
            landingssideStatusRequest().apply {
                status shouldBe HttpStatusCode.OK

                val body = deserialize<LandingssideStatusDTO>(bodyAsText())

                body.harInnsendteMeldekort shouldBe false
                body.meldekortTilUtfylling shouldHaveSize 3

                body.meldekortTilUtfylling[0].kanSendesFra shouldBe førsteMeldekort.meldeperiode.kanFyllesUtFraOgMed
                body.meldekortTilUtfylling[1].kanSendesFra shouldBe andreMeldekort.meldeperiode.kanFyllesUtFraOgMed
                body.meldekortTilUtfylling[2].kanSendesFra shouldBe tredjeMeldekort.meldeperiode.kanFyllesUtFraOgMed

                // Verifiser at listen faktisk er sortert stigende etter kanSendesFra
                body.meldekortTilUtfylling.zipWithNext { a, b ->
                    (a.kanSendesFra <= b.kanSendesFra) shouldBe true
                }

                body.redirectUrl shouldBe Configuration.meldekortFrontendUrl
            }
        }
    }
}
