package no.nav.tiltakspenger.routes.korrigering.hentkorrigering

import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.meldekort.domene.tilMeldekortTilBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.toDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.routes.saksbehandling.mottaSakRequest
import no.nav.tiltakspenger.routes.sendinnmeldekort.sendInnNesteMeldekort
import no.nav.tiltakspenger.routes.withTestApplicationContext
import org.junit.jupiter.api.Test

class HentKorrigeringRouteTest {

    private val periode = 6 til 19.januar(2025)

    @Test
    fun `hentKorrigeringForId - returnerer korrigeringsdata for innsendt meldekort`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val (sak, innsendtMeldekort) = run {
                mottaSakRequest(
                    tac = tac,
                    meldeperioder = listOf(meldeperiodeDto(periode = periode)),
                )
                sendInnNesteMeldekort(tac = tac)!!
            }

            val korrigering = hentKorrigeringForIdRequest(meldekortId = innsendtMeldekort.id.toString())!!

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
