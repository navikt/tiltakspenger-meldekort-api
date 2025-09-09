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

class AktiverMicrofrontendJobTest {
    private val sakRepo = mockk<SakRepo>(relaxed = true)
    private val tmsMikrofrontendClient = mockk<TmsMikrofrontendClient>()
    private val service = AktiverMicrofrontendJob(sakRepo, tmsMikrofrontendClient, fixedClock)

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    private val seksMånederBakover = nå(fixedClock).toLocalDate().minusMonths(6)

    @Test
    fun `aktiverer for bruker med siste meldeperiode med rett siste 6 måneder`() {
        val periode = ObjectMother.periode(fraSisteMandagFør = seksMånederBakover.plusDays(1))
        val sak = ObjectMother.sak(meldeperioder = listOf(ObjectMother.meldeperiode(periode = periode)))

        every { sakRepo.hentSakerHvorMicrofrontendSkalAktiveres(clock = fixedClock) } returns listOf(sak)
        justRun { tmsMikrofrontendClient.aktiverMicrofrontendForBruker(sak.fnr, sak.id) }

        service.aktiverMicrofrontendForBrukere()

        verify { tmsMikrofrontendClient.aktiverMicrofrontendForBruker(sak.fnr, sak.id) }
        verify { sakRepo.oppdater(sak.copy(erMicrofrontendInaktivert = false)) }
    }

    @Test
    fun `aktiverer for bruker med siste meldeperiode med rett i dag`() {
        val periode = ObjectMother.periode(fraSisteMandagFør = nå(fixedClock).toLocalDate())
        val sak = ObjectMother.sak(meldeperioder = listOf(ObjectMother.meldeperiode(periode = periode)))

        every { sakRepo.hentSakerHvorMicrofrontendSkalAktiveres(clock = fixedClock) } returns listOf(sak)
        justRun { tmsMikrofrontendClient.aktiverMicrofrontendForBruker(sak.fnr, sak.id) }

        service.aktiverMicrofrontendForBrukere()

        verify { tmsMikrofrontendClient.aktiverMicrofrontendForBruker(sak.fnr, sak.id) }
        verify { sakRepo.oppdater(sak.copy(erMicrofrontendInaktivert = false)) }
    }

    @Test
    fun `slutter ikke å forsøke aktivering selv om exception kastes ved en av aktiveringene`() {
        val periode = ObjectMother.periode(fraSisteMandagFør = seksMånederBakover.plusDays(1))
        val sak1 = ObjectMother.sak(meldeperioder = listOf(ObjectMother.meldeperiode(periode = periode)))
        val sak2 = ObjectMother.sak(meldeperioder = listOf(ObjectMother.meldeperiode(periode = periode)))

        every { sakRepo.hentSakerHvorMicrofrontendSkalAktiveres(clock = fixedClock) } returns listOf(sak1, sak2)
        every { tmsMikrofrontendClient.aktiverMicrofrontendForBruker(sak1.fnr, sak1.id) } throws RuntimeException("Feil")
        justRun { tmsMikrofrontendClient.aktiverMicrofrontendForBruker(sak2.fnr, sak2.id) }

        service.aktiverMicrofrontendForBrukere()

        verify { tmsMikrofrontendClient.aktiverMicrofrontendForBruker(sak1.fnr, sak1.id) }
        verify(exactly = 1) { sakRepo.oppdater(sak2.copy(erMicrofrontendInaktivert = false)) }
    }
}
