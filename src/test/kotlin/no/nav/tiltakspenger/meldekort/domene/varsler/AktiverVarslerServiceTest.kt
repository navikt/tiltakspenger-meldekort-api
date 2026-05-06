package no.nav.tiltakspenger.meldekort.domene.varsler

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.tiltakspenger.fakes.VarselRepoFake
import no.nav.tiltakspenger.fakes.clients.TmsVarselClientFake
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.domene.VarselId
import no.nav.tiltakspenger.meldekort.repository.SakVarselRepo
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AktiverVarslerServiceTest {
    @Test
    fun `aktiverer oppgave-varsel med nytt meldekort-builder`() {
        val nå = LocalDateTime.of(2025, 3, 10, 10, 0)
        val clock = fixedClockAt(nå)
        val varselRepo = VarselRepoFake(clock)
        val varselClient = TmsVarselClientFake()
        val service = AktiverVarslerService(
            varselRepo = varselRepo,
            sakVarselRepo = mockk<SakVarselRepo>(relaxed = true),
            varselClient = varselClient,
            sessionFactory = sessionFactory(),
            clock = clock,
        )
        val varsel = Varsel.SkalAktiveres(
            sakId = SakId.random(),
            saksnummer = "SAK-123",
            fnr = Fnr.fromString("12345678911"),
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = nå.minusHours(1),
            skalAktiveresEksterntTidspunkt = nå.plusHours(1),
            skalAktiveresBegrunnelse = "test",
            opprettet = nå.minusHours(2),
            sistEndret = nå.minusHours(2),
        )
        varselRepo.lagre(varsel)

        service.aktiverVarsler()

        varselClient.hentSendteVarsler().single().type shouldBe TmsVarselClientFake.Varseltype.NyttMeldekort
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
