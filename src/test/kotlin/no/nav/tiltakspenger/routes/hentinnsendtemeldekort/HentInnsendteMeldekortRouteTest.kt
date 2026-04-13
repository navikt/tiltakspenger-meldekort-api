package no.nav.tiltakspenger.routes.hentinnsendtemeldekort

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.domene.MeldekortMedSisteMeldeperiodeDTO
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.tilMeldeperiodeDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.routes.hentbruker.hentBrukerRequest
import no.nav.tiltakspenger.routes.saksbehandling.mottaSakRequest
import no.nav.tiltakspenger.routes.sendinnmeldekort.sendInnNesteMeldekort
import no.nav.tiltakspenger.routes.withTestApplicationContext
import org.junit.jupiter.api.Test

class HentInnsendteMeldekortRouteTest {

    private val periode = 6 til 19.januar(2025)

    @Test
    fun `hentAlleInnsendteMeldekort - returnerer innsendt meldekort`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )
            val (sak, innsendtMeldekort) = sendInnNesteMeldekort(tac = tac)!!

            val alleMeldekort = hentAlleInnsendteMeldekortRequest()!!

            alleMeldekort.shouldBe(
                bruker = hentBrukerRequest()!!,
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
