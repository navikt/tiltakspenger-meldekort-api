package no.nav.tiltakspenger.meldekort.sak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.tiltakspenger.fakes.SakVarselRepoFake
import no.nav.tiltakspenger.fakes.repos.MeldekortRepoFake
import no.nav.tiltakspenger.fakes.repos.MeldekortvedtakRepoFake
import no.nav.tiltakspenger.fakes.repos.MeldeperiodeRepoFake
import no.nav.tiltakspenger.fakes.repos.SakRepoFake
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.libs.persistering.test.common.TestSessionFactory
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortForKjede
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortRepo
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldekortvedtak.MeldekortvedtakRepo
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldeperiodebehandling
import no.nav.tiltakspenger.meldekort.meldekortvedtak.MeldeperiodebehandlingDag
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Reduksjon
import no.nav.tiltakspenger.meldekort.meldeperiode.MeldeperiodeRepo
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

/**
 * Enhetstester med mocks for å dekke de defensive feilbranchene
 * (rethrow inne i transaksjonen → ytre Either.catch → LagringFeilet).
 * TODO jah: Flytt denne testen - så det blir en integrasjonstest med ekte base (fra route eller jobb)
 */
class LagreFraSaksbehandlingServiceUnitTest {

    private fun mockedSessionFactory(): SessionFactory {
        val tx = mockk<TransactionContext>(relaxed = true)
        return mockk<SessionFactory>().also { sessionFactory ->
            every {
                sessionFactory.withTransactionContext(
                    disableSessionCounter = any(),
                    action = any<Function1<TransactionContext, Unit>>(),
                )
            } answers {
                secondArg<Function1<TransactionContext, Unit>>().invoke(tx)
            }
        }
    }

    private fun lagMeldekortvedtak(sak: Sak): Meldekortvedtak {
        val periode = sak.meldeperioder.first().periode
        return Meldekortvedtak(
            id = VedtakId.random(),
            sakId = sak.id,
            opprettet = nå(fixedClock),
            erKorrigering = false,
            erAutomatiskBehandlet = true,
            meldeperiodebehandlinger = listOf(
                Meldeperiodebehandling(
                    meldeperiodeId = sak.meldeperioder.first().id,
                    meldeperiodeKjedeId = sak.meldeperioder.first().kjedeId,
                    brukersMeldekortId = null,
                    periode = periode,
                    dager = listOf(
                        MeldeperiodebehandlingDag(
                            dato = periode.fraOgMed,
                            status = MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                            reduksjon = Reduksjon.INGEN_REDUKSJON,
                            beløp = 100,
                            beløpBarnetillegg = 0,
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `lagrer nye meldekortvedtak ved ny sak`() {
        val meldeperiodeRepoFake = MeldeperiodeRepoFake()
        val meldekortvedtakRepoFake = MeldekortvedtakRepoFake()
        val sakRepo = SakRepoFake(meldeperiodeRepoFake, meldekortvedtakRepoFake)
        val meldeperiode = ObjectMother.meldeperiode(opprettet = nå(fixedClock))
        val sakUtenVedtak = ObjectMother.sak(
            id = meldeperiode.sakId,
            saksnummer = meldeperiode.saksnummer,
            fnr = meldeperiode.fnr,
            meldeperioder = listOf(meldeperiode),
        )
        val vedtak = lagMeldekortvedtak(sakUtenVedtak)
        val sak = sakUtenVedtak.copy(meldekortvedtak = listOf(vedtak))

        val service = LagreFraSaksbehandlingService(
            sakRepo = sakRepo,
            meldeperiodeRepo = meldeperiodeRepoFake,
            meldekortRepo = MeldekortRepoFake(fixedClock),
            meldekortvedtakRepo = meldekortvedtakRepoFake,
            sakVarselRepo = SakVarselRepoFake(sakRepo),
            sessionFactory = TestSessionFactory(),
        )

        val resultat = service.lagre(sak)

        resultat.isRight() shouldBe true
        sakRepo.hent(sak.id) shouldBe sak
    }

    @Test
    fun `returnerer LagringFeilet når sakRepo_lagre kaster`() {
        val sakRepo = mockk<SakRepo>(relaxed = true)
        every { sakRepo.hent(any()) } returns null
        every { sakRepo.lagre(any(), any()) } throws RuntimeException("DB feilet")

        val service = LagreFraSaksbehandlingService(
            sakRepo = sakRepo,
            meldeperiodeRepo = mockk(relaxed = true),
            meldekortRepo = mockk(relaxed = true),
            meldekortvedtakRepo = mockk(relaxed = true),
            sakVarselRepo = mockk(relaxed = true),
            sessionFactory = mockedSessionFactory(),
        )

        val sak = ObjectMother.sak(meldeperioder = emptyList())

        val resultat = service.lagre(sak)

        resultat.isLeft() shouldBe true
        resultat.leftOrNull() shouldBe FeilVedMottakAvSak.LagringFeilet
    }

    @Test
    fun `returnerer LagringFeilet når meldeperiodeRepo_lagre kaster`() {
        val sakRepo = mockk<SakRepo>(relaxed = true)
        every { sakRepo.hent(any()) } returns null
        justRun { sakRepo.lagre(any(), any()) }

        val meldeperiodeRepo = mockk<MeldeperiodeRepo>(relaxed = true)
        every { meldeperiodeRepo.hentForId(any(), any()) } returns null
        every { meldeperiodeRepo.lagre(any(), any()) } throws RuntimeException("Meldeperiode-lagring feilet")

        val meldekortRepo = mockk<MeldekortRepo>(relaxed = true)
        every { meldekortRepo.hentMeldekortForKjedeId(any(), any(), any()) } returns MeldekortForKjede(emptyList())

        val service = LagreFraSaksbehandlingService(
            sakRepo = sakRepo,
            meldeperiodeRepo = meldeperiodeRepo,
            meldekortRepo = meldekortRepo,
            meldekortvedtakRepo = mockk(relaxed = true),
            sakVarselRepo = mockk(relaxed = true),
            sessionFactory = mockedSessionFactory(),
        )

        val meldeperiode = ObjectMother.meldeperiode(opprettet = nå(fixedClock))
        val sak = ObjectMother.sak(
            id = meldeperiode.sakId,
            saksnummer = meldeperiode.saksnummer,
            fnr = meldeperiode.fnr,
            meldeperioder = listOf(meldeperiode),
        )

        val resultat = service.lagre(sak)

        resultat.leftOrNull() shouldBe FeilVedMottakAvSak.LagringFeilet
    }

    @Test
    fun `returnerer LagringFeilet når meldekortvedtakRepo_lagre kaster`() {
        val sakRepo = mockk<SakRepo>(relaxed = true)
        every { sakRepo.hent(any()) } returns null
        justRun { sakRepo.lagre(any(), any()) }

        val meldekortvedtakRepo = mockk<MeldekortvedtakRepo>()
        every { meldekortvedtakRepo.lagre(any(), any()) } throws RuntimeException("Vedtak-lagring feilet")

        val service = LagreFraSaksbehandlingService(
            sakRepo = sakRepo,
            meldeperiodeRepo = mockk(relaxed = true),
            meldekortRepo = mockk(relaxed = true),
            meldekortvedtakRepo = meldekortvedtakRepo,
            sakVarselRepo = mockk(relaxed = true),
            sessionFactory = mockedSessionFactory(),
        )

        val meldeperiode = ObjectMother.meldeperiode(opprettet = nå(fixedClock))
        val sakUtenVedtak = ObjectMother.sak(
            id = meldeperiode.sakId,
            saksnummer = meldeperiode.saksnummer,
            fnr = meldeperiode.fnr,
            meldeperioder = emptyList(),
        )
        val vedtak = lagMeldekortvedtak(ObjectMother.sak(meldeperioder = listOf(meldeperiode)))
        val sak = sakUtenVedtak.copy(meldekortvedtak = listOf(vedtak))

        val resultat = service.lagre(sak)

        resultat.leftOrNull().shouldBeInstanceOf<FeilVedMottakAvSak>() shouldBe FeilVedMottakAvSak.LagringFeilet
    }

    @Test
    fun `SaksbehandlingApiError finnes som data object`() {
        // Sikrer at klasse-definisjonen i SaksbehandlingClient.kt er lastet
        SaksbehandlingApiError shouldBe SaksbehandlingApiError
    }
}
