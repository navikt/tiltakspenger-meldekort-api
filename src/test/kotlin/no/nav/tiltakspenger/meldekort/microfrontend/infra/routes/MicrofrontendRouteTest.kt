package no.nav.tiltakspenger.meldekort.microfrontend.infra.routes

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContext
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode.Companion.kanFyllesUtFraOgMed
import no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottaSakRequest
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import org.junit.jupiter.api.Test

class MicrofrontendRouteTest {

    private val periode = 6 til 19.januar(2025)

    @Test
    fun `microfrontendKortInfo - returnerer info om meldekort klare til innsending`() {
        withTestApplicationContextAndPostgres(clock = tikkendeKlokke1mars2025()) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
                runJobs = false,
            )

            microfrontendKortInfoRequest(fnr = sak.fnr.verdi)!!.shouldBe(
                antallMeldekortKlarTilInnsending = 1,
                nesteMuligeInnsendingstidspunkt = periode.kanFyllesUtFraOgMed(),
            )
        }
    }

    @Test
    fun `microfrontendKortInfo - svarer 500 når henting av meldekort-info feiler`() {
        withTestApplicationContext { _ ->
            microfrontendKortInfoRequest(
                fnr = "12345678901",
                forventetStatus = HttpStatusCode.InternalServerError,
                forventetContentType = null,
            )
        }
    }
}
