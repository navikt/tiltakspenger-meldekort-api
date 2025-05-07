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
import no.nav.tiltakspenger.meldekort.domene.MeldekortTilBrukerDTO
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
            path("/meldekort/bruker/neste")
        },
    )

    @Test
    fun `hent neste meldekort for utfylling`() {
        val tac = TestApplicationContext()

        val førstePeriode = Periode(
            fraOgMed = LocalDate.of(2025, 1, 6),
            tilOgMed = LocalDate.of(2025, 1, 19),
        )

        val førsteMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = førstePeriode,
        )

        val andreMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = Periode(
                fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
            ),
        )

        tac.meldeperiodeRepo.lagre(førsteMeldekort.meldeperiode)
        tac.meldekortRepo.opprett(førsteMeldekort)

        tac.meldeperiodeRepo.lagre(andreMeldekort.meldeperiode)
        tac.meldekortRepo.opprett(andreMeldekort)

        testMedMeldekortRoutes(tac) {
            nesteMeldekortRequest().apply {
                val meldekortFraBody = deserialize<MeldekortTilBrukerDTO>(bodyAsText())

                status shouldBe HttpStatusCode.OK
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")

                meldekortFraBody shouldBe førsteMeldekort.tilMeldekortTilBrukerDTO()
            }
        }
    }

    @Test
    fun `hent siste innsendte meldekort når ingen er klar til utfylling`() {
        val tac = TestApplicationContext()

        val førstePeriode = ObjectMother.periode(LocalDate.now().minusWeeks(2))

        val innsendtMeldekort = ObjectMother.meldekort(
            mottatt = førstePeriode.tilOgMed.atStartOfDay(),
            periode = førstePeriode,
        )

        val ikkeKlartMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = Periode(
                fraOgMed = førstePeriode.fraOgMed.plusWeeks(2),
                tilOgMed = førstePeriode.tilOgMed.plusWeeks(2),
            ),
        )

        tac.meldeperiodeRepo.lagre(innsendtMeldekort.meldeperiode)
        tac.meldekortRepo.opprett(innsendtMeldekort)

        tac.meldeperiodeRepo.lagre(ikkeKlartMeldekort.meldeperiode)
        tac.meldekortRepo.opprett(ikkeKlartMeldekort)

        testMedMeldekortRoutes(tac) {
            nesteMeldekortRequest().apply {
                val meldekortFraBody = deserialize<MeldekortTilBrukerDTO>(bodyAsText())

                status shouldBe HttpStatusCode.OK
                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")

                meldekortFraBody shouldBe innsendtMeldekort.tilMeldekortTilBrukerDTO()
            }
        }
    }

    @Test
    fun `returner not found dersom ingen meldekort er klare til utfylling eller tidligere utfylt`() {
        val tac = TestApplicationContext()

        val ikkeKlartMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = ObjectMother.periode(LocalDate.now()),
        )

        tac.meldeperiodeRepo.lagre(ikkeKlartMeldekort.meldeperiode)
        tac.meldekortRepo.opprett(ikkeKlartMeldekort)

        testMedMeldekortRoutes(tac) {
            nesteMeldekortRequest().apply {
                status shouldBe HttpStatusCode.NotFound
                bodyAsText() shouldBe ""
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

        val førsteDto = meldeperiodeDto(
            periode = periode,
        )
        val andreDto = førsteDto.copy(id = MeldeperiodeId.random().toString(), versjon = 2)

        meldeperiodeService.lagreFraSaksbehandling(førsteDto)
        meldeperiodeService.lagreFraSaksbehandling(andreDto)

        testMedMeldekortRoutes(tac) {
            nesteMeldekortRequest().apply {
                val meldekort = deserialize<MeldekortTilBrukerDTO>(bodyAsText())

                status shouldBe HttpStatusCode.OK

                meldekort.meldeperiodeId shouldBe andreDto.id
                meldekort.versjon shouldBe 2
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

        val førsteDto = meldeperiodeDto(
            periode = periode,
        )
        val andreDto = førsteDto.copy(
            id = MeldeperiodeId.random().toString(),
            versjon = 2,
            girRett = periode.tilDager().associateWith { false },
        )

        meldeperiodeService.lagreFraSaksbehandling(førsteDto)
        meldeperiodeService.lagreFraSaksbehandling(andreDto)

        testMedMeldekortRoutes(tac) {
            nesteMeldekortRequest().apply {
                status shouldBe HttpStatusCode.NotFound
            }
        }
    }
}
