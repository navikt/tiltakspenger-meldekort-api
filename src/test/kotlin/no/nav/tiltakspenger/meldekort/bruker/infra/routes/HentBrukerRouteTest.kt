package no.nav.tiltakspenger.meldekort.bruker.infra.routes

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContext
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortDagStatusDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.MeldekortStatusDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.sendInnNesteMeldekort
import no.nav.tiltakspenger.meldekort.sak.infra.routes.mottaSakRequest
import no.nav.tiltakspenger.objectmothers.ObjectMother.FAKE_FNR
import no.nav.tiltakspenger.objectmothers.ObjectMother.arenaMeldekort
import no.nav.tiltakspenger.objectmothers.ObjectMother.arenaMeldekortOversikt
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HentBrukerRouteTest {

    private val fnr = Fnr.fromString(FAKE_FNR)
    private val periode = 6 til 19.januar(2025)

    @Test
    fun `hentBruker - returnerer bruker med neste meldekort som kan utfylles`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )
            hentBrukerMedSakRequest()!!.shouldBe(
                nesteMeldekort = forventetMeldekort(
                    periode = periode,
                    meldeperiodeId = sak.meldeperioder.first().id,
                ),
            )
        }
    }

    @Test
    fun `hentBruker - returnerer bruker med innsendt meldekort som forrige`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )
            sendInnNesteMeldekort(tac = tac)

            hentBrukerMedSakRequest()!!.shouldBe(
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
    fun `hentBruker - bruker uten sak og uten arena-meldekort - returnerer HAR_IKKE_MELDEKORT`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { _ ->
            hentBrukerUtenSakRequest()!!.shouldBe(
                arenaMeldekortStatus = ArenaMeldekortStatusDTO.HAR_IKKE_MELDEKORT,
            )
        }
    }

    @Test
    fun `hentBruker - bruker uten sak med tiltakspenger-meldekort i arena - returnerer HAR_MELDEKORT`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortOversikt(meldekortListe = listOf(arenaMeldekort(periode = periode))),
            )

            hentBrukerUtenSakRequest()!!.shouldBe(
                arenaMeldekortStatus = ArenaMeldekortStatusDTO.HAR_MELDEKORT,
            )
        }
    }

    @Test
    fun `hentBruker - bruker uten sak med historisk tiltakspenger-meldekort i arena - returnerer HAR_MELDEKORT`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            // Aktive meldekort eksisterer, men ingen er tiltakspenger - tvinger oppslag mot historikk
            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortOversikt(
                    meldekortListe = listOf(arenaMeldekort(periode = periode, erTiltakspengerMeldekort = false)),
                ),
            )
            tac.arenaMeldekortClient.leggTilHistoriskMeldekort(
                fnr,
                arenaMeldekortOversikt(meldekortListe = listOf(arenaMeldekort(periode = periode))),
            )

            hentBrukerUtenSakRequest()!!.shouldBe(
                arenaMeldekortStatus = ArenaMeldekortStatusDTO.HAR_MELDEKORT,
            )
        }
    }

    @Test
    fun `hentBruker - bruker uten sak når arena-kallet feiler - returnerer UKJENT`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            tac.arenaMeldekortClient.leggTilMeldekortFeil(fnr)

            hentBrukerUtenSakRequest()!!.shouldBe(
                arenaMeldekortStatus = ArenaMeldekortStatusDTO.UKJENT,
            )
        }
    }

    @Test
    fun `hentBruker - bruker uten sak når historisk arena-kall feiler - returnerer UKJENT`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            // Aktive meldekort uten tiltakspenger tvinger oppslag mot historikk, som feiler
            tac.arenaMeldekortClient.leggTilMeldekort(
                fnr,
                arenaMeldekortOversikt(
                    meldekortListe = listOf(arenaMeldekort(periode = periode, erTiltakspengerMeldekort = false)),
                ),
            )
            tac.arenaMeldekortClient.leggTilHistoriskMeldekortFeil(fnr)

            hentBrukerUtenSakRequest()!!.shouldBe(
                arenaMeldekortStatus = ArenaMeldekortStatusDTO.UKJENT,
            )
        }
    }

    @Test
    fun `hentBruker - returnerer unauthorized uten token`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )
            hentBrukerMedSakRequest(
                jwt = null,
                forventetStatus = HttpStatusCode.Unauthorized,
                forventetBody = "",
                forventetContentType = null,
            )
        }
    }
}
