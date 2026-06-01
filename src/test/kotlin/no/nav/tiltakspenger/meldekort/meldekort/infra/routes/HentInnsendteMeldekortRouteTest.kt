package no.nav.tiltakspenger.meldekort.meldekort.infra.routes

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContext
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortMedSisteMeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.meldeperiode.infra.tilMeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.sak.infra.routes.mottaSakRequest
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import org.junit.jupiter.api.Test

class HentInnsendteMeldekortRouteTest {

    private val periode = 6 til 19.januar(2025)

    @Test
    fun `hentAlleInnsendteMeldekort - returnerer innsendt meldekort`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )
            val (_, innsendtMeldekort) = sendInnNesteMeldekort(tac = tac, fnr = sak.fnr.verdi)!!

            val json = hentAlleInnsendteMeldekortRequest(fnr = sak.fnr.verdi)!!

            json.shouldBeAlleMeldekortJson(
                forrigeMeldekort = innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock),
                meldekortMedSisteMeldeperiode = listOf(
                    MeldekortMedSisteMeldeperiodeDTO(
                        meldekort = innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock),
                        sisteMeldeperiode = sak.meldeperioder.last().tilMeldeperiodeDTO(),
                    ),
                ),
            )
        }
    }
}
