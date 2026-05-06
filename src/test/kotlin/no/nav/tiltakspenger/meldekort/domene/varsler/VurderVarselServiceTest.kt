package no.nav.tiltakspenger.meldekort.domene.varsler

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tiltakspenger.fakes.BeskjedVarselRepoFake
import no.nav.tiltakspenger.fakes.VarselRepoFake
import no.nav.tiltakspenger.fakes.clients.TmsVarselClientFake
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.repository.SakVarselRepo
import no.nav.tiltakspenger.meldekort.repository.VarselMeldekortRepo
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VurderVarselServiceTest {
    private val sakId = SakId.random()
    private val fnr = Fnr.fromString("12345678911")
    private val sakForVarselvurdering = SakForVarselvurdering(
        sakId = sakId,
        saksnummer = "SAK-123",
        fnr = fnr,
        sistFlaggetTidspunkt = null,
    )

    @Test
    fun `sender fire-and-forget-beskjed for endrede meldeperioder uten aa opprette oppgave`() {
        val nå = LocalDateTime.of(2025, 3, 10, 10, 0)
        val sakVarselRepo = mockk<SakVarselRepo>(relaxed = true).also {
            every { it.hentSakerSomSkalVurdereVarsel() } returns listOf(sakForVarselvurdering)
        }
        val varselMeldekortRepo = varselMeldekortRepo(
            beskjeder = listOf(
                BeskjedMeldeperiode(
                    sakId = sakId,
                    meldeperiodeId = MeldeperiodeId.random(),
                    kjedeId = MeldeperiodeKjedeId("2025-03-10/2025-03-23"),
                    versjon = 2,
                    sisteInnsendteVersjon = 1,
                    endring = MeldeperiodeEndring(
                        maksAntallDagerForPeriode = Verdiendring(fra = 10, til = 8),
                        girRett = listOf(
                            GirRettEndring(
                                dato = LocalDate.of(2025, 3, 12),
                                fra = true,
                                til = false,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val beskjedVarselRepo = BeskjedVarselRepoFake()
        val varselRepo = VarselRepoFake(fixedClockAt(nå))
        val varselClient = TmsVarselClientFake()
        val service = VurderVarselService(
            sakVarselRepo = sakVarselRepo,
            varselMeldekortRepo = varselMeldekortRepo,
            varselRepo = varselRepo,
            beskjedVarselRepo = beskjedVarselRepo,
            varselClient = varselClient,
            sessionFactory = sessionFactory(),
            clock = fixedClockAt(nå),
        )

        service.vurderVarsler()

        beskjedVarselRepo.hentAlle().single().meldeperioder shouldHaveSize 1
        varselRepo.hentVarslerForSakId(sakId) shouldHaveSize 0
        varselClient.hentSendteVarsler().single().type shouldBe TmsVarselClientFake.Varseltype.MeldeperiodeEndret
        verify { sakVarselRepo.markerVarselVurdert(sakId, nå, null, any()) }
    }

    private fun varselMeldekortRepo(
        beskjeder: List<BeskjedMeldeperiode>,
        oppgaver: List<KjedeSomManglerInnsending> = emptyList(),
    ): VarselMeldekortRepo = object : VarselMeldekortRepo {
        override fun hentKjederSomManglerInnsending(
            sakId: SakId,
            sessionContext: SessionContext?,
        ): List<KjedeSomManglerInnsending> = oppgaver

        override fun hentMeldeperioderSomSkalHaBeskjed(
            sakId: SakId,
            sessionContext: SessionContext?,
        ): List<BeskjedMeldeperiode> = beskjeder
    }

    private fun sessionFactory(): SessionFactory {
        val transactionContext = mockk<TransactionContext>(relaxed = true)
        return mockk<SessionFactory>().also { sessionFactory ->
            every {
                sessionFactory.withTransactionContext(
                    disableSessionCounter = any(),
                    action = any<Function1<TransactionContext, Unit>>(),
                )
            } answers {
                secondArg<Function1<TransactionContext, Unit>>().invoke(transactionContext)
            }
        }
    }
}
