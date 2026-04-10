package no.nav.tiltakspenger.routes.hentbruker

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatusDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortStatusDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.routes.saksbehandling.mottaSakRequest
import no.nav.tiltakspenger.routes.sendinnmeldekort.sendInnNesteMeldekort
import no.nav.tiltakspenger.routes.withTestApplicationContext
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HentBrukerRouteTest {

    private val periode = 6 til 19.januar(2025)

    @Test
    fun `hentBruker - returnerer bruker med neste meldekort som kan utfylles`() {
        runTest {
            withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
                val sak = mottaSakRequest(
                    tac = tac,
                    meldeperioder = listOf(meldeperiodeDto(periode = periode)),
                )
                hentBrukerRequest()!!.shouldBe(
                    nesteMeldekort = forventetMeldekort(
                        periode = periode,
                        meldeperiodeId = sak.meldeperioder.first().id,
                    ),
                )
            }
        }
    }

    @Test
    fun `hentBruker - returnerer bruker med innsendt meldekort som forrige`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode)),
            )
            sendInnNesteMeldekort(tac = tac)

            hentBrukerRequest()!!.shouldBe(
                forrigeMeldekort = forventetMeldekort(
                    periode = periode,
                    meldeperiodeId = sak.meldeperioder.first().id,
                    status = MeldekortStatusDTO.INNSENDT,
                    innsendt = LocalDateTime.MIN,
                    dager = forventedeDager(
                        periode = periode,
                        hverdagStatus = MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET,
                    ),
                    kanSendes = null,
                ),
            )
        }
    }

    @Test
    fun `hentBruker - returnerer unauthorized uten token`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode)),
            )
            hentBrukerRequest(
                jwt = null,
                forventetStatus = HttpStatusCode.Unauthorized,
                forventetBody = "",
                forventetContentType = null,
            )
        }
    }
}
