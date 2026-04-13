package no.nav.tiltakspenger.meldekort.domene.varsler

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.meldekort.clients.varsler.SendtVarselMetadata
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClient
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SendVarslerServiceTest {
    private val meldekortRepo = mockk<MeldekortRepo>(relaxed = true)
    private val tmsVarselClient = mockk<TmsVarselClient>(relaxed = true)
    private val service = SendVarslerService(meldekortRepo, tmsVarselClient, fixedClock)
    private val forventetTidspunkt = nå(fixedClock)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `sender varsel med riktig varselId og tidspunkt`() {
        val varselId = VarselId.random()
        val meldekort = ObjectMother.meldekort(varselId = varselId)

        every { meldekortRepo.hentMeldekortDetSkalVarslesFor() } returns listOf(meldekort)
        every {
            tmsVarselClient.sendVarselForNyttMeldekort(
                meldekort,
                varselId,
            )
        } returns SendtVarselMetadata("\"json-request\"")

        service.sendVarselForMeldekort()

        verify(exactly = 1) { tmsVarselClient.sendVarselForNyttMeldekort(meldekort, varselId) }
        verify(exactly = 1) { meldekortRepo.markerVarslet(meldekort.id, forventetTidspunkt, any()) }
    }

    @Test
    fun `sender varsler for flere meldekort`() {
        val meldekortList = (1..3).map { ObjectMother.meldekort(fnr = Fnr.random()) }

        every { meldekortRepo.hentMeldekortDetSkalVarslesFor() } returns meldekortList
        every {
            tmsVarselClient.sendVarselForNyttMeldekort(
                any(),
                any(),
            )
        } returns SendtVarselMetadata("\"json-request\"")

        service.sendVarselForMeldekort()

        meldekortList.forEach { meldekort ->
            verify(exactly = 1) { tmsVarselClient.sendVarselForNyttMeldekort(meldekort, meldekort.varselId) }
            verify(exactly = 1) { meldekortRepo.markerVarslet(meldekort.id, forventetTidspunkt, any()) }
        }
    }

    @Test
    fun `gjør ingenting når det ikke finnes meldekort å varsle for`() {
        every { meldekortRepo.hentMeldekortDetSkalVarslesFor() } returns emptyList()

        service.sendVarselForMeldekort()

        verify(exactly = 0) { tmsVarselClient.sendVarselForNyttMeldekort(any(), any()) }
        verify(exactly = 0) { meldekortRepo.markerVarslet(any(), any(), any()) }
    }

    @Test
    fun `markerer ikke varsel som sendt når sending feiler`() {
        val meldekort = ObjectMother.meldekort()

        every { meldekortRepo.hentMeldekortDetSkalVarslesFor() } returns listOf(meldekort)
        every { tmsVarselClient.sendVarselForNyttMeldekort(any(), any()) } throws RuntimeException("Kafka feil")

        service.sendVarselForMeldekort()

        verify(exactly = 1) { tmsVarselClient.sendVarselForNyttMeldekort(meldekort, meldekort.varselId) }
        verify(exactly = 0) { meldekortRepo.markerVarslet(any(), any(), any()) }
    }

    @Test
    fun `fortsetter å sende varsler for resterende meldekort når ett feiler`() {
        val feiletMeldekort = ObjectMother.meldekort(fnr = Fnr.random())
        val vellykketMeldekort = ObjectMother.meldekort(fnr = Fnr.random())

        every { meldekortRepo.hentMeldekortDetSkalVarslesFor() } returns listOf(feiletMeldekort, vellykketMeldekort)
        every {
            tmsVarselClient.sendVarselForNyttMeldekort(
                feiletMeldekort,
                feiletMeldekort.varselId,
            )
        } throws RuntimeException("Feil")
        every {
            tmsVarselClient.sendVarselForNyttMeldekort(
                vellykketMeldekort,
                vellykketMeldekort.varselId,
            )
        } returns SendtVarselMetadata("\"json-request\"")

        service.sendVarselForMeldekort()

        verify(exactly = 0) { meldekortRepo.markerVarslet(feiletMeldekort.id, any(), any()) }
        verify(exactly = 1) { meldekortRepo.markerVarslet(vellykketMeldekort.id, forventetTidspunkt, any()) }
    }

    @Test
    fun `fortsetter å sende varsler når lagring av markert varsel feiler for et meldekort`() {
        val meldekortMedLagringsfeil = ObjectMother.meldekort(fnr = Fnr.random())
        val vellykketMeldekort = ObjectMother.meldekort(fnr = Fnr.random())
        val sendtVarselMetadata = SendtVarselMetadata("\"json-request\"")

        every {
            meldekortRepo.hentMeldekortDetSkalVarslesFor()
        } returns listOf(meldekortMedLagringsfeil, vellykketMeldekort)
        every {
            tmsVarselClient.sendVarselForNyttMeldekort(meldekortMedLagringsfeil, meldekortMedLagringsfeil.varselId)
        } returns sendtVarselMetadata
        every {
            tmsVarselClient.sendVarselForNyttMeldekort(vellykketMeldekort, vellykketMeldekort.varselId)
        } returns sendtVarselMetadata
        every {
            meldekortRepo.markerVarslet(meldekortMedLagringsfeil.id, forventetTidspunkt, sendtVarselMetadata)
        } throws RuntimeException("Database feil")

        service.sendVarselForMeldekort()

        verify(exactly = 1) {
            tmsVarselClient.sendVarselForNyttMeldekort(meldekortMedLagringsfeil, meldekortMedLagringsfeil.varselId)
        }
        verify(exactly = 1) {
            tmsVarselClient.sendVarselForNyttMeldekort(vellykketMeldekort, vellykketMeldekort.varselId)
        }
        verify(exactly = 1) {
            meldekortRepo.markerVarslet(meldekortMedLagringsfeil.id, forventetTidspunkt, sendtVarselMetadata)
        }
        verify(exactly = 1) {
            meldekortRepo.markerVarslet(vellykketMeldekort.id, forventetTidspunkt, sendtVarselMetadata)
        }
    }

    @Test
    fun `håndterer feil ved henting av meldekort fra repo`() {
        every { meldekortRepo.hentMeldekortDetSkalVarslesFor() } throws RuntimeException("Database feil")

        service.sendVarselForMeldekort()

        verify(exactly = 0) { tmsVarselClient.sendVarselForNyttMeldekort(any(), any()) }
        verify(exactly = 0) { meldekortRepo.markerVarslet(any(), any(), any()) }
    }
}
