package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContext
import no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottaSakRequest
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import org.junit.jupiter.api.Test

class HentMeldekortRouteTest {

    private val periode = 6 til 19.januar(2025)

    @Test
    fun `hentMeldekortForId - returnerer spesifikt meldekort`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )
            val (_, innsendtMeldekort) = sendInnNesteMeldekort(tac = tac, fnr = sak.fnr.verdi)!!
            val actualMeldekort = hentMeldekortForIdRequest(fnr = sak.fnr.verdi, meldekortId = innsendtMeldekort.id)!!

            actualMeldekort
                .erInnsendt()
                .harAntallDager(14)
                .harKjedeId(innsendtMeldekort.meldeperiode.kjedeId)
        }
    }
}
