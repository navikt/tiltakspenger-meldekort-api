package no.nav.tiltakspenger.routes.korrigering.kankorrigeres

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.routes.saksbehandling.mottaSakRequest
import no.nav.tiltakspenger.routes.sendinnmeldekort.sendInnNesteMeldekort
import no.nav.tiltakspenger.routes.withTestApplicationContext
import org.junit.jupiter.api.Test

class KanKorrigeresRouteTest {

    private val periode = 6 til 19.januar(2025)

    @Test
    fun `kanMeldekortKorrigeres - returnerer true for innsendt meldekort`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode)),
            )
            val (_, innsendtMeldekort) = sendInnNesteMeldekort(tac = tac)!!

            kanMeldekortKorrigeresRequest(meldekortId = innsendtMeldekort.id.toString())!!.shouldBe(
                kanKorrigeres = true,
            )
        }
    }
}
