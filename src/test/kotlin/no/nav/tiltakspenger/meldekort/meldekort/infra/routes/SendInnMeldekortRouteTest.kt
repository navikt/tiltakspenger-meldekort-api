package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.lagreMeldeperiode
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.bruker.infra.routes.hentBrukerMedSakRequest
import no.nav.tiltakspenger.meldekort.bruker.infra.routes.shouldBe
import no.nav.tiltakspenger.meldekort.infra.routes.JwtGenerator
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContext
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortFraBrukerDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottaSakRequest
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.objectmothers.lagMeldekort
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SendInnMeldekortRouteTest {

    private val periode = 6 til 19.januar(2025)
    private val januarPeriode = ObjectMother.periode(LocalDate.of(2025, 1, 1))

    @Test
    fun `sendInnMeldekort - sender inn meldekort og returnerer OK`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )

            val (_, innsendtMeldekort) = sendInnNesteMeldekort(tac = tac, fnr = sak.fnr.verdi)!!

            // Verifiser at meldekortet nå er innsendt via hentBruker
            hentBrukerMedSakRequest(fnr = sak.fnr.verdi)!!.shouldBe(
                forrigeMeldekort = innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock),
            )
        }
    }

    @Test
    fun `sendInnMeldekort - ugyldig body gir 400 med ErrorJson-feil`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()

            defaultRequestWithAssertions(
                method = HttpMethod.Post,
                uri = "/brukerfrontend/send-inn",
                jwt = JwtGenerator().createJwtForUser(fnr = fnr.verdi),
                forventet = ForventetRespons(
                    status = HttpStatusCode.BadRequest,
                    body = ForventetBody.Eksakt("""{"melding":"Vi klarte ikke å lese inn meldekortet du sendte. Prøv igjen.","kode":"ugyldig_meldekort_innsending"}"""),
                    contentType = ContentType.Application.Json,
                ),
            ) {
                setBody("dette er ikke gyldig json")
            }
        }
    }

    @Test
    fun `sendInnMeldekort - ukjent meldekort gir 404 med ErrorJson-feil`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()

            sendInnForventFeil(
                tac = tac,
                meldekortId = MeldekortId.random(),
                fnr = fnr,
                forventetStatus = HttpStatusCode.NotFound,
                forventetBody = """{"melding":"Vi fant ikke meldekortet du prøver å sende inn.","kode":"fant_ikke_meldekort"}""",
            )
        }
    }

    @Test
    fun `sendInnMeldekort - allerede mottatt meldekort gir 409 med ErrorJson-feil`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()
            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(periode = januarPeriode, fnr = fnr, opprettet = nå(tac.clock)),
                mottatt = nå(tac.clock),
            )

            sendInnForventFeil(
                tac = tac,
                meldekortId = meldekort.id,
                fnr = fnr,
                forventetStatus = HttpStatusCode.Conflict,
                forventetBody = """{"melding":"Dette meldekortet er allerede sendt inn.","kode":"meldekort_allerede_mottatt"}""",
            )
        }
    }

    @Test
    fun `sendInnMeldekort - erstattet meldeperiode gir 409 med ErrorJson-feil`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()
            val meldeperiodeV1 = ObjectMother.meldeperiode(periode = januarPeriode, fnr = fnr, opprettet = nå(tac.clock))
            val meldekort = tac.lagMeldekort(meldeperiodeV1)

            tac.lagreMeldeperiode(
                ObjectMother.meldeperiode(
                    periode = januarPeriode,
                    fnr = fnr,
                    sakId = meldeperiodeV1.sakId,
                    saksnummer = meldeperiodeV1.saksnummer,
                    versjon = meldeperiodeV1.versjon + 1,
                    opprettet = nå(tac.clock),
                ),
            )

            sendInnForventFeil(
                tac = tac,
                meldekortId = meldekort.id,
                fnr = fnr,
                forventetStatus = HttpStatusCode.Conflict,
                forventetBody = """{"melding":"Dette meldekortet er ikke lenger gyldig fordi det har kommet en nyere versjon av meldeperioden. Gå tilbake til oversikten og send inn det nyeste meldekortet.","kode":"meldekortets_meldeperiode_er_erstattet"}""",
            )
        }
    }

    @Test
    fun `sendInnMeldekort - deaktivert meldekort gir 409 med ErrorJson-feil`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()
            val meldeperiode = ObjectMother.meldeperiode(periode = januarPeriode, fnr = fnr, opprettet = nå(tac.clock))
            tac.lagreMeldeperiode(meldeperiode)
            val meldekort = ObjectMother.meldekort(
                meldeperiode = meldeperiode,
                mottatt = null,
                deaktivert = nå(tac.clock),
            )
            tac.meldekortRepo.lagre(meldekort)

            sendInnForventFeil(
                tac = tac,
                meldekortId = meldekort.id,
                fnr = fnr,
                forventetStatus = HttpStatusCode.Conflict,
                forventetBody = """{"melding":"Dette meldekortet er ikke lenger gyldig fordi det har kommet en nyere versjon av meldeperioden. Gå tilbake til oversikten og send inn det nyeste meldekortet.","kode":"meldekort_deaktivert"}""",
            )
        }
    }

    @Test
    fun `sendInnMeldekort - meldekort ikke klart til innsending gir 409 med ErrorJson-feil`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()
            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(
                    periode = ObjectMother.periode(LocalDate.now(tac.clock)),
                    fnr = fnr,
                    opprettet = nå(tac.clock),
                ),
            )

            sendInnForventFeil(
                tac = tac,
                meldekortId = meldekort.id,
                fnr = fnr,
                forventetStatus = HttpStatusCode.Conflict,
                forventetBody = """{"melding":"Dette meldekortet er ikke klart til innsending ennå. Prøv igjen senere.","kode":"meldekort_ikke_klart_til_innsending"}""",
            )
        }
    }

    @Test
    fun `sendInnMeldekort - manglende meldeperiode gir 500 med ErrorJson-feil`() = runTest {
        withTestApplicationContext { tac ->
            val fnr = tac.nesteFnr()
            // Lagrer meldekortet uten å lagre meldeperioden, slik at oppslaget på siste meldeperiode gir null.
            val meldekort = ObjectMother.meldekort(
                meldeperiode = ObjectMother.meldeperiode(periode = januarPeriode, fnr = fnr, opprettet = nå(tac.clock)),
                mottatt = null,
            )
            tac.meldekortRepo.lagre(meldekort)

            sendInnForventFeil(
                tac = tac,
                meldekortId = meldekort.id,
                fnr = fnr,
                forventetStatus = HttpStatusCode.InternalServerError,
                forventetBody = """{"melding":"Det oppstod en feil med meldekortet ditt. Ta kontakt med oss hvis problemet vedvarer.","kode":"fant_ikke_meldeperiode"}""",
            )
        }
    }

    @Test
    fun `sendInnMeldekort - uventet feil under utfylling gir 500 med ErrorJson-feil`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val fnr = tac.nesteFnr()
            // Meldekortet er klart til innsending, men tomme dager (sendt via hjelperen) bryter en domeneinvariant under utfylling og gir en uventet feil.
            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(periode = januarPeriode, fnr = fnr, opprettet = nå(tac.clock)),
            )

            sendInnForventFeil(
                tac = tac,
                meldekortId = meldekort.id,
                fnr = fnr,
                forventetStatus = HttpStatusCode.InternalServerError,
                forventetBody = """{"melding":"Det oppstod en uventet feil ved innsending av meldekortet. Prøv igjen senere.","kode":"uventet_feil_ved_lagring"}""",
            )
        }
    }

    private suspend fun ApplicationTestBuilder.sendInnForventFeil(
        tac: TestApplicationContext,
        meldekortId: MeldekortId,
        fnr: Fnr,
        forventetStatus: HttpStatusCode,
        forventetBody: String,
    ) {
        sendInnMeldekortRequest(
            tac = tac,
            requestDto = MeldekortFraBrukerDTO(id = meldekortId.toString(), dager = emptyList(), locale = null),
            fnr = fnr,
            jwt = JwtGenerator().createJwtForUser(fnr = fnr.verdi),
            runJobs = false,
            forventetStatus = forventetStatus,
            forventetBody = forventetBody,
            forventetContentType = ContentType.Application.Json,
        )
    }
}
