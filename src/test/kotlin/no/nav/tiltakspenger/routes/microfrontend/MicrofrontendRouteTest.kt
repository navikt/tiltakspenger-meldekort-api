package no.nav.tiltakspenger.routes.microfrontend

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode.Companion.kanFyllesUtFraOgMed
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.routes.saksbehandling.mottaSakRequest
import no.nav.tiltakspenger.routes.withTestApplicationContext
import org.junit.jupiter.api.Test

class MicrofrontendRouteTest {

    private val periode = 6 til 19.januar(2025)

    @Test
    fun `microfrontendKortInfo - returnerer info om meldekort klare til innsending`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )

            microfrontendKortInfoRequest()!!.shouldBe(
                antallMeldekortKlarTilInnsending = 1,
                nesteMuligeInnsendingstidspunkt = periode.kanFyllesUtFraOgMed(),
            )
        }
    }
}
