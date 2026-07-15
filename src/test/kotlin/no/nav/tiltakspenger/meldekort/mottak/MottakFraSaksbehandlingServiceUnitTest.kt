package no.nav.tiltakspenger.meldekort.mottak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Meldeperiodebehandling
import no.nav.tiltakspenger.meldekort.meldekortvedtak.MeldeperiodebehandlingDag
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Reduksjon
import no.nav.tiltakspenger.meldekort.sak.Sak
import no.nav.tiltakspenger.meldekort.sak.SakRepo
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.tilMottattSak
import org.junit.jupiter.api.Test

/**
 * Enhetstester med mocks for de defensive feilbranchene der en write inne i transaksjonen kaster
 * (rethrow → ytre Either.catch → [FeilVedMottakAvSak.LagringFeilet]).
 *
 * Disse to grenene (sak-lagring og meldekortvedtak-lagring kaster) er ikke realistisk reproduserbare mot en ekte base via det naturlige mottaks-flytet, og dekkes derfor med mocks.
 * Den ene grenen som *er* reproduserbar mot ekte Postgres (databasekonstraint-brudd ved meldeperiode-insert) dekkes som ende-til-ende-test i [no.nav.tiltakspenger.meldekort.mottak.infra.routes.MottakFraSaksbehandlingEndToEndTest].
 *
 * TODO: Når servicen ryddes opp (fjerne de indre defensive catch-ene), kan denne mock-baserte testen fjernes.
 */
class MottakFraSaksbehandlingServiceUnitTest {

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
    fun `returnerer LagringFeilet når sakRepo_lagre kaster`() {
        val sakRepo = mockk<SakRepo>(relaxed = true)
        every { sakRepo.hent(any()) } returns null
        val mottakRepo = mockk<MottakRepo>(relaxed = true)
        every { mottakRepo.lagreSak(any(), any()) } throws RuntimeException("DB feilet")

        val service = MottakFraSaksbehandlingService(
            sakRepo = sakRepo,
            meldeperiodeRepo = mockk(relaxed = true),
            meldekortRepo = mockk(relaxed = true),
            sakVarselRepo = mockk(relaxed = true),
            mottakRepo = mottakRepo,
            sessionFactory = mockedSessionFactory(),
        )

        val sak = ObjectMother.sak(meldeperioder = emptyList())

        val resultat = service.lagre(sak.tilMottattSak())

        resultat.isLeft() shouldBe true
        resultat.leftOrNull() shouldBe FeilVedMottakAvSak.LagringFeilet
    }

    @Test
    fun `returnerer LagringFeilet når meldekortvedtakRepo_lagre kaster`() {
        val sakRepo = mockk<SakRepo>(relaxed = true)
        every { sakRepo.hent(any()) } returns null

        val mottakRepo = mockk<MottakRepo>(relaxed = true)
        justRun { mottakRepo.lagreSak(any(), any()) }
        every { mottakRepo.lagreMeldekortvedtak(any(), any()) } throws RuntimeException("Vedtak-lagring feilet")

        val service = MottakFraSaksbehandlingService(
            sakRepo = sakRepo,
            meldeperiodeRepo = mockk(relaxed = true),
            meldekortRepo = mockk(relaxed = true),
            sakVarselRepo = mockk(relaxed = true),
            mottakRepo = mottakRepo,
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

        val resultat = service.lagre(sak.tilMottattSak())

        resultat.leftOrNull().shouldBeInstanceOf<FeilVedMottakAvSak>() shouldBe FeilVedMottakAvSak.LagringFeilet
    }
}
