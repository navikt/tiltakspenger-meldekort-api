package no.nav.tiltakspenger.routes.hentmeldekortforkjede

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.routes.saksbehandling.mottaSakRequest
import no.nav.tiltakspenger.routes.sendinnmeldekort.sendInnNesteMeldekort
import no.nav.tiltakspenger.routes.withTestApplicationContext
import org.junit.jupiter.api.Test

class HentMeldekortForKjedeRouteTest {

    private val periode = 6 til 19.januar(2025)

    @Test
    fun `hentMeldekortForKjedeId - returnerer meldekort for kjede`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode)),
            )
            val (_, innsendtMeldekort) = sendInnNesteMeldekort(tac = tac)!!

            // kjedeId inneholder "/" som må erstattes med "_" i URL
            val kjedeId = innsendtMeldekort.meldeperiode.kjedeId.toString()
            val urlSafeKjedeId = kjedeId.replace("/", "_")

            val kjedeDto = hentMeldekortForKjedeIdRequest(kjedeId = urlSafeKjedeId)!!

            kjedeDto.shouldBe(
                kjedeId = kjedeId,
                periode = innsendtMeldekort.meldeperiode.periode.toDTO(),
                meldekort = listOf(innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock)),
            )
        }
    }
}
