package no.nav.tiltakspenger.meldekort.domene.microfrontend

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.meldekort.clients.microfrontend.TmsMikrofrontendClient
import no.nav.tiltakspenger.meldekort.repository.SakRepo
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InaktiverMicrofrontendJobTest {
    private val sakRepo = mockk<SakRepo>(relaxed = true)
    private val tmsMikrofrontendClient = mockk<TmsMikrofrontendClient>()
    private val service = InaktiverMicrofrontendJob(sakRepo, tmsMikrofrontendClient, fixedClock)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    private val seksMånederBakover = nå(fixedClock).toLocalDate().minusMonths(6)

    @Test
    fun `inaktiverer for bruker med siste meldeperiode med rett 6 måneder bakover i tid`() {
        val periode = ObjectMother.periode(fraSisteMandagFør = seksMånederBakover)
        val sak = ObjectMother.sak(meldeperioder = listOf(ObjectMother.meldeperiode(periode = periode)))

        every { sakRepo.hentSakerHvorSistePeriodeMedRettighetErLengeSiden(clock = fixedClock) } returns listOf(sak)
        justRun { tmsMikrofrontendClient.inaktiverMicrofrontendForBruker(sak.fnr, sak.id) }

        service.inaktiverMicrofrontendForBrukere()

        verify { tmsMikrofrontendClient.inaktiverMicrofrontendForBruker(sak.fnr, sak.id) }
        verify { sakRepo.oppdaterErMicrofrontendInaktivert(sakId = sak.id, erMicrofrontendInaktivert = true) }
    }

    @Test
    fun `slutter ikke å forsøke inaktivering selv om exception kastes ved en av inaktiveringene`() {
        val periode = ObjectMother.periode(fraSisteMandagFør = seksMånederBakover)
        val sak1 = ObjectMother.sak(meldeperioder = listOf(ObjectMother.meldeperiode(periode = periode)))
        val sak2 = ObjectMother.sak(meldeperioder = listOf(ObjectMother.meldeperiode(periode = periode)))

        every { sakRepo.hentSakerHvorSistePeriodeMedRettighetErLengeSiden(clock = fixedClock) } returns listOf(sak1, sak2)
        every { tmsMikrofrontendClient.inaktiverMicrofrontendForBruker(sak1.fnr, sak1.id) } throws RuntimeException("Feil")
        justRun { tmsMikrofrontendClient.inaktiverMicrofrontendForBruker(sak2.fnr, sak2.id) }

        service.inaktiverMicrofrontendForBrukere()

        verify { tmsMikrofrontendClient.inaktiverMicrofrontendForBruker(sak1.fnr, sak1.id) }
        verify(exactly = 1) { sakRepo.oppdaterErMicrofrontendInaktivert(sakId = sak2.id, erMicrofrontendInaktivert = true) }
    }
}
