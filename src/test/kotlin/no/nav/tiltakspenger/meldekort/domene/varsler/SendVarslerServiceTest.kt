package no.nav.tiltakspenger.meldekort.domene.varsler

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClient
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.time.LocalDate
import java.util.UUID

class SendVarslerServiceTest {
    private val meldekortRepo = mockk<MeldekortRepo>(relaxed = true)
    private val tmsVarselClient = mockk<TmsVarselClient>(relaxed = true)
    private val service = SendVarslerService(meldekortRepo, tmsVarselClient)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `oppretter varsler for meldekort`() {
        val meldekort = ObjectMother.meldekort()
        val varselId = slot<VarselId>()

        every { meldekortRepo.hentDeSomIkkeHarBlittVarsletFor() } returns listOf(meldekort)
        justRun { tmsVarselClient.sendVarselForNyttMeldekort(meldekort, capture(varselId)) }

        service.sendVarselForMeldekort()

        verify { meldekortRepo.oppdater(any()) }
        assertNotNull(UUID.fromString(varselId.captured.toString()), "VarselId ble satt til en gyldig UUID")
        verify { tmsVarselClient.sendVarselForNyttMeldekort(meldekort, varselId.captured) }
    }

    @Test
    fun `oppretter varsler for flere meldekort`() {
        val meldekort1 = ObjectMother.meldekort(fnr = Fnr.random())
        val meldekort2 = ObjectMother.meldekort(fnr = Fnr.random())
        val meldekort3 = ObjectMother.meldekort(fnr = Fnr.random())
        val meldekortList = listOf(meldekort1, meldekort2, meldekort3)

        every { meldekortRepo.hentDeSomIkkeHarBlittVarsletFor() } returns meldekortList
        justRun { tmsVarselClient.sendVarselForNyttMeldekort(any(), any()) }

        service.sendVarselForMeldekort()

        verify(exactly = meldekortList.size) { meldekortRepo.oppdater(any()) }
        verify(exactly = meldekortList.size) { tmsVarselClient.sendVarselForNyttMeldekort(any(), any()) }
    }

    @Test
    fun `bruker har flere meldekort - sender bare varsel for det eldste`() {
        val fnr = Fnr.random()
        val forstePeriode = ObjectMother.periode(LocalDate.now().minusWeeks(8))
        val forsteMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = forstePeriode,
            fnr = fnr,
        )
        val andreMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = Periode(
                fraOgMed = forstePeriode.fraOgMed.plusWeeks(2),
                tilOgMed = forstePeriode.tilOgMed.plusWeeks(2),
            ),
            fnr = fnr,
        )
        val tredjeMeldekort = ObjectMother.meldekort(
            mottatt = null,
            periode = Periode(
                fraOgMed = forstePeriode.fraOgMed.plusWeeks(4),
                tilOgMed = forstePeriode.tilOgMed.plusWeeks(4),
            ),
            fnr = fnr,
        )
        val meldekortList = listOf(forsteMeldekort, andreMeldekort, tredjeMeldekort)

        every { meldekortRepo.hentDeSomIkkeHarBlittVarsletFor() } returns meldekortList
        justRun { tmsVarselClient.sendVarselForNyttMeldekort(any(), any()) }

        service.sendVarselForMeldekort()

        verify(exactly = 1) { meldekortRepo.oppdater(match { it.id == forsteMeldekort.id }) }
        verify(exactly = 1) { tmsVarselClient.sendVarselForNyttMeldekort(forsteMeldekort, any()) }
    }
}
