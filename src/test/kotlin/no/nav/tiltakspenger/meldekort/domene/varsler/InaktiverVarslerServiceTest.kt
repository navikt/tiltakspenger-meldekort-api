package no.nav.tiltakspenger.meldekort.domene.varsler

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClient
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class InaktiverVarslerServiceTest {

    private val meldekortRepo = mockk<MeldekortRepo>(relaxed = true)
    private val tmsVarselClient = mockk<TmsVarselClient>(relaxed = true)
    private val service = InaktiverVarslerService(meldekortRepo, tmsVarselClient)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `inaktiverer varsler for mottatte meldekort med varsler`() {
        val varselId = VarselId("varsel1")
        val meldekort = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = varselId)

        every { meldekortRepo.hentMottatteEllerDeaktiverteSomDetVarslesFor() } returns listOf(meldekort)

        service.inaktiverVarslerForMottatteMeldekort()

        verify { tmsVarselClient.inaktiverVarsel(varselId) }
        verify { meldekortRepo.oppdater(meldekort.copy(erVarselInaktivert = true)) }
    }

    @Test
    fun `slutter ikke å forsøke inaktivering av varsling om det oppstår feil ved inaktivering av varsel`() {
        val varselId = VarselId("varsel1")
        val meldekort = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = varselId)

        every { meldekortRepo.hentMottatteEllerDeaktiverteSomDetVarslesFor() } returns listOf(meldekort)

        service.inaktiverVarslerForMottatteMeldekort()

        verify { tmsVarselClient.inaktiverVarsel(varselId) }
        verify(exactly = 0) { meldekortRepo.opprett(any()) }
    }

    @Test
    fun `slutter ikke å forsøke inaktivering av varsling dersom exception kastes ved inaktivering av varsel`() {
        val varselId = VarselId("varsel1")
        val meldekort = ObjectMother.meldekort(mottatt = LocalDateTime.now(), varselId = varselId)

        every { meldekortRepo.hentMottatteEllerDeaktiverteSomDetVarslesFor() } returns listOf(meldekort)
        every { tmsVarselClient.inaktiverVarsel(varselId) } throws RuntimeException("Feil")

        service.inaktiverVarslerForMottatteMeldekort()

        verify { tmsVarselClient.inaktiverVarsel(varselId) }
        verify(exactly = 0) { meldekortRepo.opprett(any()) }
    }
}
