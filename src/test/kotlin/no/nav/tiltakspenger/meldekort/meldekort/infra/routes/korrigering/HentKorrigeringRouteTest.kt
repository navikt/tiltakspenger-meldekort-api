package no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContext
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.sendInnNesteMeldekort
import no.nav.tiltakspenger.meldekort.meldekort.infra.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.toDto
import no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottaSakRequest
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import org.junit.jupiter.api.Test

class HentKorrigeringRouteTest {

    private val periode = 6 til 19.januar(2025)

    @Test
    fun `hentKorrigeringForId - returnerer korrigeringsdata for innsendt meldekort`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val (sak, innsendtMeldekort) = run {
                val sak = mottaSakRequest(
                    tac = tac,
                    meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
                )
                sendInnNesteMeldekort(tac = tac, fnr = sak.fnr.verdi)!!
            }

            val korrigering = hentKorrigeringForIdRequest(fnr = sak.fnr.verdi, meldekortId = innsendtMeldekort.id.toString())!!

            korrigering.shouldBe(
                forrigeMeldekort = innsendtMeldekort.tilMeldekortTilBrukerDTO(tac.clock),
                tilUtfylling = forventetPreutfyltKorrigering(
                    meldeperiodeId = innsendtMeldekort.meldeperiode.id.toString(),
                    kjedeId = innsendtMeldekort.meldeperiode.kjedeId.toString(),
                    dager = innsendtMeldekort.dager.toDto(),
                    periode = innsendtMeldekort.meldeperiode.periode.toDTO(),
                    mottattTidspunktSisteMeldekort = requireNotNull(innsendtMeldekort.mottatt),
                    maksAntallDagerForPeriode = innsendtMeldekort.meldeperiode.maksAntallDagerForPeriode,
                    kanSendeInnHelg = sak.kanSendeInnHelgForMeldekort,
                ),
            )
        }
    }
}
