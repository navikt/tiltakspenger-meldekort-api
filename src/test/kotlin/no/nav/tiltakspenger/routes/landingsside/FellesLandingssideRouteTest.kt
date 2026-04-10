package no.nav.tiltakspenger.routes.landingsside

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.routes.saksbehandling.mottaSakRequest
import no.nav.tiltakspenger.routes.withTestApplicationContext
import org.junit.jupiter.api.Test

class FellesLandingssideRouteTest {

    @Test
    fun `landingssideStatus - returnerer status for bruker med meldekort`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = 6 til 19.januar(2025))),
            )
            landingssideStatusRequest()!!.shouldBe(
                meldekortTilUtfylling = listOf(17.januar(2025).atTime(15, 0, 0)),
            )
        }
    }
}
