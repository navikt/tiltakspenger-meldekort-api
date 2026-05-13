package no.nav.tiltakspenger.meldekort.varsler

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import no.nav.tiltakspenger.fakes.SakVarselRepoFake
import no.nav.tiltakspenger.fakes.VarselRepoFake
import no.nav.tiltakspenger.fakes.clients.TmsVarselClientFake
import no.nav.tiltakspenger.fakes.repos.SakRepoFake
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.varsler.VarselId
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class InaktiverVarslerServiceTest {
    private val nå = LocalDateTime.of(2025, 1, 6, 12, 0)
    private val clock = fixedClockAt(nå)

    @Test
    fun `inaktiverer alle forfalte SkalInaktiveres for samme sak`() {
        val sakId = SakId.random()
        val fnr = Fnr.random()
        val varselRepo = VarselRepoFake(clock)
        val varselClient = TmsVarselClientFake()
        val første = skalInaktiveres(
            sakId = sakId,
            saksnummer = "sak-1",
            fnr = fnr,
            skalAktiveresTidspunkt = LocalDateTime.of(2025, 1, 6, 9, 0),
            opprettet = LocalDateTime.of(2025, 1, 6, 9, 0),
        )
        val andre = skalInaktiveres(
            sakId = sakId,
            saksnummer = "sak-1",
            fnr = fnr,
            skalAktiveresTidspunkt = LocalDateTime.of(2025, 1, 6, 10, 0),
            opprettet = LocalDateTime.of(2025, 1, 6, 10, 0),
        )
        varselRepo.lagre(første)
        varselRepo.lagre(andre)
        val service = InaktiverVarslerService(
            varselRepo = varselRepo,
            sakVarselRepo = SakVarselRepoFake(SakRepoFake()),
            varselClient = varselClient,
            sessionFactory = sessionFactory(),
            clock = clock,
        )

        service.inaktiverVarsler()

        val lagredeVarsler = varselRepo.hentVarslerForSakId(sakId)
        lagredeVarsler shouldHaveSize 2
        lagredeVarsler.forEach { it.shouldBeInstanceOf<Varsel.Inaktivert>() }
        varselClient.hentInaktiverteVarsler() shouldContainExactlyInAnyOrder listOf(første.varselId, andre.varselId)
    }

    private fun skalInaktiveres(
        sakId: SakId,
        saksnummer: String,
        fnr: Fnr,
        skalAktiveresTidspunkt: LocalDateTime,
        opprettet: LocalDateTime,
    ): Varsel.SkalInaktiveres {
        return Varsel.Aktiv(
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            skalAktiveresEksterntTidspunkt = skalAktiveresTidspunkt,
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = skalAktiveresTidspunkt.plusMinutes(1),
            eksternAktiveringstidspunkt = skalAktiveresTidspunkt.plusMinutes(1),
            opprettet = opprettet,
            sistEndret = skalAktiveresTidspunkt.plusMinutes(1),
        ).forberedInaktivering(
            skalInaktiveresTidspunkt = skalAktiveresTidspunkt.plusHours(1),
            skalInaktiveresBegrunnelse = "test",
        )
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
