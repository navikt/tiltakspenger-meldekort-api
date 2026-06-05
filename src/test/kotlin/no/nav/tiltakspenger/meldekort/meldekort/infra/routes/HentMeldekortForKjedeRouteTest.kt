package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContext
import no.nav.tiltakspenger.meldekort.meldekort.infra.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottaSakRequest
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import org.junit.jupiter.api.Test

class HentMeldekortForKjedeRouteTest {

    private val periode = 6 til 19.januar(2025)

    @Test
    fun `hentMeldekortForKjedeId - returnerer meldekort for kjede`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )
            val (_, innsendtMeldekort) = sendInnNesteMeldekort(tac = tac, fnr = sak.fnr.verdi)!!

            // kjedeId inneholder "/" som må erstattes med "_" i URL
            val kjedeId = innsendtMeldekort.meldeperiode.kjedeId.toString()
            val urlSafeKjedeId = kjedeId.replace("/", "_")

            val kjedeDto = hentMeldekortForKjedeIdRequest(fnr = sak.fnr.verdi, kjedeId = urlSafeKjedeId)!!

            kjedeDto.shouldBe(
                kjedeId = kjedeId,
                periode = innsendtMeldekort.meldeperiode.periode.toDTO(),
                meldekort = listOf(innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock)),
            )
        }
    }
}
