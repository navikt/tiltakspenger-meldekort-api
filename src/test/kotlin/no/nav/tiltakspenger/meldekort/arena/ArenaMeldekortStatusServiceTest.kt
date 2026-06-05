package no.nav.tiltakspenger.meldekort.arena

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.fakes.clients.ArenaMeldekortClientFake
import no.nav.tiltakspenger.fakes.repos.SakRepoFake
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.sak.Sak
import no.nav.tiltakspenger.meldekort.sak.SakRepo
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class ArenaMeldekortStatusServiceTest {
    private val periode = Periode(6.januar(2025), 19.januar(2025))

    private fun service(
        client: ArenaMeldekortClientFake,
        sakRepo: SakRepo = SakRepoFake(),
    ) = ArenaMeldekortStatusService(arenaMeldekortClient = client, sakRepo = sakRepo)

    private fun oversiktMed(erTiltakspengerMeldekort: Boolean) = ObjectMother.arenaMeldekortOversikt(
        meldekortListe = listOf(ObjectMother.arenaMeldekort(periode = periode, erTiltakspengerMeldekort = erTiltakspengerMeldekort)),
    )

    @Test
    fun `feil ved henting av aktive meldekort gir UKJENT`() = runTest {
        val fnr = Fnr.random()
        val client = ArenaMeldekortClientFake().apply { leggTilMeldekortFeil(fnr) }

        service(client).hentArenaMeldekortStatus(fnr) shouldBe ArenaMeldekortStatus.UKJENT
    }

    @Test
    fun `ingen meldekort i arena gir HAR_IKKE_MELDEKORT`() = runTest {
        val fnr = Fnr.random()

        service(ArenaMeldekortClientFake()).hentArenaMeldekortStatus(fnr) shouldBe ArenaMeldekortStatus.HAR_IKKE_MELDEKORT
    }

    @Test
    fun `aktivt tiltakspenger-meldekort gir HAR_MELDEKORT`() = runTest {
        val fnr = Fnr.random()
        val client = ArenaMeldekortClientFake().apply { leggTilMeldekort(fnr, oversiktMed(erTiltakspengerMeldekort = true)) }

        service(client).hentArenaMeldekortStatus(fnr) shouldBe ArenaMeldekortStatus.HAR_MELDEKORT
    }

    @Test
    fun `kun ikke-tiltakspenger aktivt, feil på historiske gir UKJENT`() = runTest {
        val fnr = Fnr.random()
        val client = ArenaMeldekortClientFake().apply {
            leggTilMeldekort(fnr, oversiktMed(erTiltakspengerMeldekort = false))
            leggTilHistoriskMeldekortFeil(fnr)
        }

        service(client).hentArenaMeldekortStatus(fnr) shouldBe ArenaMeldekortStatus.UKJENT
    }

    @Test
    fun `historisk tiltakspenger-meldekort gir HAR_MELDEKORT`() = runTest {
        val fnr = Fnr.random()
        val client = ArenaMeldekortClientFake().apply {
            leggTilMeldekort(fnr, oversiktMed(erTiltakspengerMeldekort = false))
            leggTilHistoriskMeldekort(fnr, oversiktMed(erTiltakspengerMeldekort = true))
        }

        service(client).hentArenaMeldekortStatus(fnr) shouldBe ArenaMeldekortStatus.HAR_MELDEKORT
    }

    @Test
    fun `ingen tiltakspenger-meldekort verken aktivt eller historisk gir HAR_IKKE_MELDEKORT`() = runTest {
        val fnr = Fnr.random()
        val client = ArenaMeldekortClientFake().apply {
            leggTilMeldekort(fnr, oversiktMed(erTiltakspengerMeldekort = false))
            leggTilHistoriskMeldekort(fnr, oversiktMed(erTiltakspengerMeldekort = false))
        }

        service(client).hentArenaMeldekortStatus(fnr) shouldBe ArenaMeldekortStatus.HAR_IKKE_MELDEKORT
    }

    @Test
    fun `oppdaterArenaMeldekortStatusForSaker oppdaterer kun saker med kjent status`() = runTest {
        val sakRepo = SakRepoFake()
        val sakMedMeldekort = ObjectMother.mottattSak()
        val sakUtenAvklaring = ObjectMother.mottattSak()
        sakRepo.lagre(sakMedMeldekort)
        sakRepo.lagre(sakUtenAvklaring)

        val client = ArenaMeldekortClientFake().apply {
            leggTilMeldekort(sakMedMeldekort.fnr, oversiktMed(erTiltakspengerMeldekort = true))
            leggTilMeldekortFeil(sakUtenAvklaring.fnr)
        }

        service(client, sakRepo).oppdaterArenaMeldekortStatusForSaker()

        sakRepo.hent(sakMedMeldekort.id)!!.arenaMeldekortStatus shouldBe ArenaMeldekortStatus.HAR_MELDEKORT
        sakRepo.hent(sakUtenAvklaring.id)!!.arenaMeldekortStatus shouldBe ArenaMeldekortStatus.UKJENT
    }

    @Test
    fun `feil under oppdatering av en sak svelges og stopper ikke jobben`() = runTest {
        val sakRepo = SakRepoFake()
        val sak = ObjectMother.mottattSak()
        sakRepo.lagre(sak)
        val kastendeRepo = object : SakRepo by sakRepo {
            override fun oppdaterArenaStatus(id: SakId, arenaStatus: ArenaMeldekortStatus, sessionContext: SessionContext?): Unit =
                throw RuntimeException("oppdatering feilet")
        }
        val client = ArenaMeldekortClientFake().apply {
            leggTilMeldekort(sak.fnr, oversiktMed(erTiltakspengerMeldekort = true))
        }

        // Skal ikke kaste.
        service(client, kastendeRepo).oppdaterArenaMeldekortStatusForSaker()
    }

    @Test
    fun `feil ved henting av saker svelges`() = runTest {
        val kastendeRepo = object : SakRepo by SakRepoFake() {
            override fun hentSakerUtenArenaStatus(sessionContext: SessionContext?): List<Sak> =
                throw RuntimeException("henting feilet")
        }

        // Skal ikke kaste.
        service(ArenaMeldekortClientFake(), kastendeRepo).oppdaterArenaMeldekortStatusForSaker()
    }

    @Test
    fun `arena-datatyper kan konstrueres`() {
        ArenaFravaerType(fraDato = 1.januar(2025), tilDato = 2.januar(2025), type = "ORDINAER") shouldBe
            ArenaFravaerType(fraDato = 1.januar(2025), tilDato = 2.januar(2025), type = "ORDINAER")
        ArenaMeldekortServiceFeil.HttpFeil(status = 500).status shouldBe 500
    }
}
