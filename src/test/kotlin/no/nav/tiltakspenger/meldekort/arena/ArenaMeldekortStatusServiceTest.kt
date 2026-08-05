package no.nav.tiltakspenger.meldekort.arena

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.Level
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.Loggfanger
import no.nav.tiltakspenger.fakes.clients.ArenaMeldekortClientFake
import no.nav.tiltakspenger.fakes.repos.SakRepoFake
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.jobb.JobbResultat
import no.nav.tiltakspenger.meldekort.sak.Sak
import no.nav.tiltakspenger.meldekort.sak.SakRepo
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class ArenaMeldekortStatusServiceTest {
    private val periode = Periode(6.januar(2025), 19.januar(2025))

    private fun service(
        client: ArenaMeldekortClientFake,
        sakRepo: SakRepo = SakRepoFake(),
        logger: KLogger = Loggfanger(ArenaMeldekortStatusService::class.java.name),
    ) = ArenaMeldekortStatusService(arenaMeldekortClient = client, sakRepo = sakRepo, logger = logger)

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
        val loggfanger = Loggfanger(ArenaMeldekortStatusService::class.java.name)

        // Jobben skal ikke kaste, men fullføre og melde fra om at den utførte arbeid.
        service(client, kastendeRepo, loggfanger).oppdaterArenaMeldekortStatusForSaker() shouldBe JobbResultat.UtførteArbeid

        // At feilen ble svelget *og logget* er hele poenget med testen, så den påstanden må sjekkes.
        loggfanger.linjerPå(Level.WARN).single().also {
            it.melding shouldContain "Feil under oppdatering av Arena-meldekortstatus for sak ${sak.id}"
            it.årsakskjede() shouldContain "oppdatering feilet"
        }
        loggfanger.linjerPå(Level.ERROR).shouldBeEmpty()
    }

    @Test
    fun `feil ved henting av saker svelges`() = runTest {
        val kastendeRepo = object : SakRepo by SakRepoFake() {
            override fun hentSakerUtenArenaStatus(sessionContext: SessionContext?): List<Sak> =
                throw RuntimeException("henting feilet")
        }
        val loggfanger = Loggfanger(ArenaMeldekortStatusService::class.java.name)

        // Jobben skal ikke kaste, men rapportere at den feilet.
        service(ArenaMeldekortClientFake(), kastendeRepo, loggfanger).oppdaterArenaMeldekortStatusForSaker() shouldBe JobbResultat.Feilet

        loggfanger.linjerPå(Level.ERROR).single().also {
            it.melding shouldContain "Ukjent feil skjedde under oppdatering av Arena-meldekortstatus for saker"
            it.årsakskjede() shouldContain "henting feilet"
        }
    }

    @Test
    fun `arena-datatyper kan konstrueres`() {
        ArenaFravaerType(fraDato = 1.januar(2025), tilDato = 2.januar(2025), type = "ORDINAER") shouldBe
            ArenaFravaerType(fraDato = 1.januar(2025), tilDato = 2.januar(2025), type = "ORDINAER")
    }
}
