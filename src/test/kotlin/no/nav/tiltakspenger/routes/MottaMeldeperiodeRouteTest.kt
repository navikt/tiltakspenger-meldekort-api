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
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeDTO
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.meldekort.domene.tilMeldeperiode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.lagreMeldekortFraBrukerKommando
import org.junit.jupiter.api.Test

class MottaMeldeperiodeRouteTest {
    private suspend fun ApplicationTestBuilder.mottaMeldeperiodeRequest(dto: MeldeperiodeDTO) = defaultRequest(
        HttpMethod.Post,
        url {
            protocol = URLProtocol.HTTPS
            path("/saksbehandling/meldeperiode")
        },
    ) {
        setBody(serialize(dto))
    }

    @Test
    fun `Skal lagre meldeperioden og opprette meldekort`() {
        val tac = TestApplicationContext()

        val dto = ObjectMother.meldeperiodeDto()
        val id = MeldeperiodeId.fromString(dto.id)

        testMedMeldekortRoutes(tac) {
            mottaMeldeperiodeRequest(dto).apply {
                status shouldBe HttpStatusCode.OK

                tac.meldeperiodeRepo.hentForId(id) shouldBe dto.tilMeldeperiode().getOrFail()
            }
        }
    }

    @Test
    fun `Skal håndtere duplikate requests for lagring av meldeperiode og returnere ok`() {
        val tac = TestApplicationContext()

        val dto = ObjectMother.meldeperiodeDto()
        val id = MeldeperiodeId.fromString(dto.id)

        testMedMeldekortRoutes(tac) {
            mottaMeldeperiodeRequest(dto).apply {
                status shouldBe HttpStatusCode.OK
            }

            mottaMeldeperiodeRequest(dto).apply {
                status shouldBe HttpStatusCode.OK

                tac.meldeperiodeRepo.hentForId(id) shouldBe dto.tilMeldeperiode().getOrFail()
            }
        }
    }

    @Test
    fun `Skal håndtere requests for lagring av samme meldeperiode med ulike data og returnere 409`() {
        val tac = TestApplicationContext()

        val dto = ObjectMother.meldeperiodeDto()
        val id = MeldeperiodeId.fromString(dto.id)

        val dtoMedDiff = dto.copy(opprettet = dto.opprettet.minusDays(1))

        testMedMeldekortRoutes(tac) {
            mottaMeldeperiodeRequest(dto).apply {
                status shouldBe HttpStatusCode.OK
            }

            mottaMeldeperiodeRequest(dtoMedDiff).apply {
                status shouldBe HttpStatusCode.Conflict

                val meldeperiode = tac.meldeperiodeRepo.hentForId(id)!!

                meldeperiode shouldBe dto.tilMeldeperiode().getOrFail()
                meldeperiode.opprettet shouldBe dto.opprettet
            }
        }
    }

    @Test
    fun `Skal opprette nytt meldekort og deaktivere forrige ved ny meldeperiode-versjon`() {
        val tac = TestApplicationContext()

        val dtoFørste = ObjectMother.meldeperiodeDto()
        val meldeperiodeFørste = dtoFørste.tilMeldeperiode().getOrFail()

        val dtoAndre = dtoFørste.copy(
            id = MeldeperiodeId.random().toString(),
            versjon = 2,
        )
        val meldeperiodeAndre = dtoAndre.tilMeldeperiode().getOrFail()

        testMedMeldekortRoutes(tac) {
            mottaMeldeperiodeRequest(dtoFørste).apply {
                status shouldBe HttpStatusCode.OK

                tac.meldeperiodeRepo.hentForId(meldeperiodeFørste.id) shouldBe meldeperiodeFørste
            }

            mottaMeldeperiodeRequest(dtoAndre).apply {
                status shouldBe HttpStatusCode.OK

                tac.meldeperiodeRepo.hentForId(meldeperiodeAndre.id) shouldBe meldeperiodeAndre

                val (førsteMeldekort, andreMeldekort) =
                    tac.meldekortRepo.hentMeldekortForKjedeId(meldeperiodeAndre.kjedeId, meldeperiodeAndre.fnr)

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

        val dtoFørste = ObjectMother.meldeperiodeDto()
        val meldeperiodeFørste = dtoFørste.tilMeldeperiode().getOrFail()

        val dtoAndre = dtoFørste.copy(
            id = MeldeperiodeId.random().toString(),
            versjon = 2,
        )
        val meldeperiodeAndre = dtoAndre.tilMeldeperiode().getOrFail()

        testMedMeldekortRoutes(tac) {
            mottaMeldeperiodeRequest(dtoFørste).apply {
                status shouldBe HttpStatusCode.OK

                tac.meldeperiodeRepo.hentForId(meldeperiodeFørste.id) shouldBe meldeperiodeFørste

                val meldekort =
                    tac.meldekortRepo.hentMeldekortForKjedeId(meldeperiodeFørste.kjedeId, meldeperiodeFørste.fnr)
                        .first()

                val lagreKommando =
                    lagreMeldekortFraBrukerKommando(meldeperiode = meldeperiodeFørste, meldekortId = meldekort.id)

                tac.meldekortRepo.lagreFraBruker(lagreKommando)
            }

            mottaMeldeperiodeRequest(dtoAndre).apply {
                status shouldBe HttpStatusCode.OK

                tac.meldeperiodeRepo.hentForId(meldeperiodeAndre.id) shouldBe meldeperiodeAndre

                val (førsteMeldekort, andreMeldekort) =
                    tac.meldekortRepo.hentMeldekortForKjedeId(meldeperiodeAndre.kjedeId, meldeperiodeAndre.fnr)

                førsteMeldekort.deaktivert shouldBe null

                andreMeldekort.deaktivert shouldBe null
                førsteMeldekort.varselId shouldBe andreMeldekort.varselId
            }
        }
    }
}
