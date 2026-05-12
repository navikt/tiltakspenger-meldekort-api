package no.nav.tiltakspenger.meldekort.sak.infra

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Reduksjon
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class SakTilMeldekortApiDTOExtTest {

    @Test
    fun `mapper alle Status- og Reduksjon-enumverdier fra DTO til domene`() {
        val periode = ObjectMother.periode()
        val dager = periode.tilDager()

        // Vi har 14 dager og 10 statuser + 3 reduksjoner — fyller opp slik at alle enumverdier dekkes.
        val statuser = SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status.entries
        val reduksjoner = SakTilMeldekortApiDTO.MeldekortvedtakDTO.Reduksjon.entries

        val dagerDTO = dager.mapIndexed { index, dato ->
            SakTilMeldekortApiDTO.MeldekortvedtakDTO.MeldeperiodebehandlingDTO.DagDTO(
                dato = dato,
                status = statuser[index % statuser.size],
                reduksjon = reduksjoner[index % reduksjoner.size],
                beløp = 100,
                beløpBarnetillegg = 0,
            )
        }

        val meldekortvedtakDTO = SakTilMeldekortApiDTO.MeldekortvedtakDTO(
            id = VedtakId.random().toString(),
            opprettet = nå(fixedClock),
            erKorrigering = false,
            erAutomatiskBehandlet = true,
            meldeperiodebehandlinger = listOf(
                SakTilMeldekortApiDTO.MeldekortvedtakDTO.MeldeperiodebehandlingDTO(
                    meldeperiodeId = no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId.random().toString(),
                    meldeperiodeKjedeId = no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId.fraPeriode(periode).toString(),
                    brukersMeldekortId = no.nav.tiltakspenger.libs.common.MeldekortId.random().toString(),
                    periodeDTO = periode.toDTO(),
                    dager = dagerDTO,
                ),
            ),
        )

        val sakDto = ObjectMother.sakDTO(meldekortvedtak = listOf(meldekortvedtakDTO))

        val sak = sakDto.tilSak()

        sak.meldekortvedtak shouldHaveSize 1
        val vedtak = sak.meldekortvedtak.single()
        vedtak.meldeperiodebehandlinger shouldHaveSize 1
        val behandling = vedtak.meldeperiodebehandlinger.single()
        behandling.dager shouldHaveSize 14

        // Verifiser at alle enumverdier ble forsøkt mappet (besøkt) ved å sjekke at
        // antall unike status- og reduksjon-verdier i resultatet matcher det vi sendte inn.
        behandling.dager.map { it.status }.toSet() shouldBe MeldekortDagStatus.entries.toSet()
        behandling.dager.map { it.reduksjon }.toSet() shouldBe Reduksjon.entries.toSet()
    }

    @Test
    fun `mapper meldekortvedtak med brukersMeldekortId = null`() {
        val periode = ObjectMother.periode()
        val dagerDTO = periode.tilDager().map { dato ->
            SakTilMeldekortApiDTO.MeldekortvedtakDTO.MeldeperiodebehandlingDTO.DagDTO(
                dato = dato,
                status = SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status.IKKE_BESVART,
                reduksjon = SakTilMeldekortApiDTO.MeldekortvedtakDTO.Reduksjon.INGEN_REDUKSJON,
                beløp = 0,
                beløpBarnetillegg = 0,
            )
        }

        val meldekortvedtakDTO = SakTilMeldekortApiDTO.MeldekortvedtakDTO(
            id = VedtakId.random().toString(),
            opprettet = nå(fixedClock),
            erKorrigering = false,
            erAutomatiskBehandlet = true,
            meldeperiodebehandlinger = listOf(
                SakTilMeldekortApiDTO.MeldekortvedtakDTO.MeldeperiodebehandlingDTO(
                    meldeperiodeId = no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId.random().toString(),
                    meldeperiodeKjedeId = no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId.fraPeriode(periode).toString(),
                    brukersMeldekortId = null,
                    periodeDTO = periode.toDTO(),
                    dager = dagerDTO,
                ),
            ),
        )

        val sak = ObjectMother.sakDTO(meldekortvedtak = listOf(meldekortvedtakDTO)).tilSak()

        sak.meldekortvedtak.single().meldeperiodebehandlinger.single().brukersMeldekortId shouldBe null
    }
}
