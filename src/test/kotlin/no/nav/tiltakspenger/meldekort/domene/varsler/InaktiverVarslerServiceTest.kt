package no.nav.tiltakspenger.meldekort.domene.varsler

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.meldekort.clients.varsler.TmsVarselClient
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.repository.MeldekortRepo
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InaktiverVarslerServiceTest {
    private val meldekortRepo = mockk<MeldekortRepo>(relaxed = true)
    private val tmsVarselClient = mockk<TmsVarselClient>()
    private val service = InaktiverVarslerService(meldekortRepo, tmsVarselClient)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `inaktiverer varsler for mottatte meldekort med varsler`() {
        val varselId = VarselId("varsel1")
        val meldekort = ObjectMother.meldekort(mottatt = nå(fixedClock), varselId = varselId)

        every { meldekortRepo.henteMeldekortSomSkalInaktivereVarsel() } returns listOf(meldekort)
        justRun { tmsVarselClient.inaktiverVarsel(varselId) }

        service.inaktiverVarslerForMottatteMeldekort()

        verify { tmsVarselClient.inaktiverVarsel(varselId) }
        verify(exactly = 1) { meldekortRepo.lagre(meldekort.copy(erVarselInaktivert = true)) }
    }

    @Test
    fun `slutter ikke å forsøke inaktivering selv om exception kastes ved en av inaktiveringene`() {
        val clock = TikkendeKlokke()
        val varselId1 = VarselId("varsel1")
        val varselId2 = VarselId("varsel2")
        val meldekort1 = ObjectMother.meldekort(mottatt = nå(clock), varselId = varselId1)
        val meldekort2 = ObjectMother.meldekort(mottatt = nå(clock), varselId = varselId2)

        every { meldekortRepo.henteMeldekortSomSkalInaktivereVarsel() } returns listOf(meldekort1, meldekort2)
        every { tmsVarselClient.inaktiverVarsel(varselId1) } throws RuntimeException("Feil")
        justRun { tmsVarselClient.inaktiverVarsel(varselId2) }

        service.inaktiverVarslerForMottatteMeldekort()

        verify { tmsVarselClient.inaktiverVarsel(varselId1) }
        verify { tmsVarselClient.inaktiverVarsel(varselId2) }
        verify(exactly = 1) { meldekortRepo.lagre(any()) }
    }

    @Test
    fun `fortsetter å forsøke inaktivering når lagring av inaktivert varsel feiler`() {
        val varselId1 = VarselId("varsel1")
        val varselId2 = VarselId("varsel2")
        val meldekort1 = ObjectMother.meldekort(mottatt = nå(fixedClock), varselId = varselId1)
        val meldekort2 = ObjectMother.meldekort(mottatt = nå(fixedClock), varselId = varselId2)

        every { meldekortRepo.henteMeldekortSomSkalInaktivereVarsel() } returns listOf(meldekort1, meldekort2)
        justRun { tmsVarselClient.inaktiverVarsel(varselId1) }
        justRun { tmsVarselClient.inaktiverVarsel(varselId2) }
        every { meldekortRepo.lagre(meldekort1.copy(erVarselInaktivert = true)) } throws RuntimeException("Database feil")

        service.inaktiverVarslerForMottatteMeldekort()

        verify(exactly = 1) { tmsVarselClient.inaktiverVarsel(varselId1) }
        verify(exactly = 1) { tmsVarselClient.inaktiverVarsel(varselId2) }
        verify(exactly = 1) { meldekortRepo.lagre(meldekort1.copy(erVarselInaktivert = true)) }
        verify(exactly = 1) { meldekortRepo.lagre(meldekort2.copy(erVarselInaktivert = true)) }
    }

    @Test
    fun `gjør ingenting når det ikke finnes varsler å inaktivere`() {
        every { meldekortRepo.henteMeldekortSomSkalInaktivereVarsel() } returns emptyList()

        service.inaktiverVarslerForMottatteMeldekort()

        verify(exactly = 0) { tmsVarselClient.inaktiverVarsel(any()) }
        verify(exactly = 0) { meldekortRepo.lagre(any()) }
    }

    @Test
    fun `håndterer feil ved henting av meldekort som skal inaktivere varsler`() {
        every { meldekortRepo.henteMeldekortSomSkalInaktivereVarsel() } throws RuntimeException("Database feil")

        service.inaktiverVarslerForMottatteMeldekort()

        verify(exactly = 0) { tmsVarselClient.inaktiverVarsel(any()) }
        verify(exactly = 0) { meldekortRepo.lagre(any()) }
    }
}
