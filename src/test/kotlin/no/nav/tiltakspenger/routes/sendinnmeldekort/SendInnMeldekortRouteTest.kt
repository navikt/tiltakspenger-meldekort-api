package no.nav.tiltakspenger.routes.sendinnmeldekort

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.routes.hentbruker.hentBrukerRequest
import no.nav.tiltakspenger.routes.hentbruker.shouldBe
import no.nav.tiltakspenger.routes.saksbehandling.mottaSakRequest
import no.nav.tiltakspenger.routes.withTestApplicationContext
import org.junit.jupiter.api.Test

class SendInnMeldekortRouteTest {

    private val periode = 6 til 19.januar(2025)

    @Test
    fun `sendInnMeldekort - sender inn meldekort og returnerer OK`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode)),
            )

            val (_, innsendtMeldekort) = sendInnNesteMeldekort(tac = tac)!!

            // Verifiser at meldekortet nå er innsendt via hentBruker
            hentBrukerRequest()!!.shouldBe(
                forrigeMeldekort = innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock),
            )
        }
    }
}
