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
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.BrukerDTO
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import org.junit.jupiter.api.Test
import java.time.LocalDate

class NesteMeldekortRouteTest {

    private suspend fun ApplicationTestBuilder.nesteMeldekortRequest() = defaultRequest(
        HttpMethod.Get,
        url {
            protocol = URLProtocol.HTTPS
            path("/brukerfrontend/bruker")
        },
    )

    @Test
    fun `hent neste meldekort for utfylling`() {
        val tac = TestApplicationContext()

        val førstePeriode = Periode(
            fraOgMed = LocalDate.of(2025, 1, 6),
            tilOgMed = LocalDate.of(2025, 1, 19),
        )

        val andrePeriode = førstePeriode.plus14Dager()

        val sak = ObjectMother.sak(
            meldeperioder = listOf(førstePeriode, andrePeriode),
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
            nesteMeldekortRequest().apply {
                val body = deserialize<BrukerDTO.MedSak>(bodyAsText())

                status shouldBe HttpStatusCode.OK
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")

                body.nesteMeldekort shouldBe førsteMeldekort.tilMeldekortTilBrukerDTO()
                body.forrigeMeldekort shouldBe null
                body.nesteMeldeperiode shouldBe ObjectMother.nesteMeldeperiodeDTO(andrePeriode)
            }
        }
    }

    @Test
    fun `hent forrige innsendte meldekort når ingen er klar til utfylling`() {
        val tac = TestApplicationContext()

        val førstePeriode = ObjectMother.periode(LocalDate.now().minusWeeks(2))
        val andrePeriode = førstePeriode.plus14Dager()

        val sak = ObjectMother.sak(
            meldeperioder = listOf(førstePeriode, andrePeriode),
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
            nesteMeldekortRequest().apply {
                val body = deserialize<BrukerDTO.MedSak>(bodyAsText())

                status shouldBe HttpStatusCode.OK
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")

                body.nesteMeldekort shouldBe null
                body.forrigeMeldekort shouldBe innsendtMeldekort.tilMeldekortTilBrukerDTO()
                body.nesteMeldeperiode shouldBe ObjectMother.nesteMeldeperiodeDTO(andrePeriode)
            }
        }
    }

    @Test
    fun `returner not found dersom ingen meldekort er klare til utfylling eller tidligere utfylt`() {
        val tac = TestApplicationContext()

        val periode = ObjectMother.periode(LocalDate.now())

        val sak = ObjectMother.sak(meldeperioder = listOf(periode))

        val ikkeKlartMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = periode,
        )

        tac.sakRepo.lagre(sak)

        tac.meldeperiodeRepo.lagre(ikkeKlartMeldekort.meldeperiode)
        tac.meldekortRepo.opprett(ikkeKlartMeldekort)

        testMedMeldekortRoutes(tac) {
            nesteMeldekortRequest().apply {
                val body = deserialize<BrukerDTO.MedSak>(bodyAsText())

                status shouldBe HttpStatusCode.OK

                body.nesteMeldekort shouldBe null
                body.forrigeMeldekort shouldBe null
                body.nesteMeldeperiode shouldBe ObjectMother.nesteMeldeperiodeDTO(periode)
            }
        }
    }

    @Test
    fun `skal hente siste versjon av meldekort for en periode med flere versjoner`() {
        val tac = TestApplicationContext()
        val meldeperiodeService = tac.meldeperiodeService

        val periode = Periode(
            fraOgMed = LocalDate.of(2025, 1, 6),
            tilOgMed = LocalDate.of(2025, 1, 19),
        )

        val sak = ObjectMother.sak(meldeperioder = listOf(periode))
        tac.sakRepo.lagre(sak)

        val førsteDto = meldeperiodeDto(
            periode = periode,
        )
        val andreDto = førsteDto.copy(id = MeldeperiodeId.random().toString(), versjon = 2)

        meldeperiodeService.lagreFraSaksbehandling(førsteDto)
        meldeperiodeService.lagreFraSaksbehandling(andreDto)

        testMedMeldekortRoutes(tac) {
            nesteMeldekortRequest().apply {
                val body = deserialize<BrukerDTO.MedSak>(bodyAsText())

                status shouldBe HttpStatusCode.OK

                body.nesteMeldekort!!.meldeperiodeId shouldBe andreDto.id
                body.nesteMeldekort.versjon shouldBe 2
                body.forrigeMeldekort shouldBe null
                body.nesteMeldeperiode shouldBe null
            }
        }
    }

    @Test
    fun `skal ikke hente deaktivert meldekort`() {
        val tac = TestApplicationContext()
        val meldeperiodeService = tac.meldeperiodeService

        val periode = Periode(
            fraOgMed = LocalDate.of(2025, 1, 6),
            tilOgMed = LocalDate.of(2025, 1, 19),
        )

        val sak = ObjectMother.sak(meldeperioder = listOf(periode))
        tac.sakRepo.lagre(sak)

        val førsteDto = meldeperiodeDto(
            periode = periode,
        )
        val andreDto = førsteDto.copy(
            id = MeldeperiodeId.random().toString(),
            versjon = 2,
            girRett = periode.tilDager().associateWith { false },
        )

        meldeperiodeService.lagreFraSaksbehandling(førsteDto)

        tac.sakService.lagreFraSaksbehandling(
            ObjectMother.sakDTO(
                fnr = sak.fnr.verdi,
                sakId = sak.id.toString(),
                saksnummer = sak.saksnummer,
                meldeperioder = emptyList(),
            ),
        )

        meldeperiodeService.lagreFraSaksbehandling(andreDto)

        testMedMeldekortRoutes(tac) {
            nesteMeldekortRequest().apply {
                val body = deserialize<BrukerDTO.MedSak>(bodyAsText())

                status shouldBe HttpStatusCode.OK

                body.nesteMeldekort shouldBe null
                body.forrigeMeldekort shouldBe null
                body.nesteMeldeperiode shouldBe null
            }
        }
    }
}
