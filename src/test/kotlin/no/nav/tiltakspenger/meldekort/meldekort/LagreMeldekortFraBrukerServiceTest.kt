package no.nav.tiltakspenger.meldekort.meldekort

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.lagreMeldeperiode
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContext
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.objectmothers.lagMeldekort
import no.nav.tiltakspenger.objectmothers.lagMeldekortFraBrukerKommando
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LagreMeldekortFraBrukerServiceTest {
    private val gyldigPeriode = ObjectMother.periode(LocalDate.of(2025, 1, 1))

    @Test
    fun `Kan lagre gyldig meldekort fra bruker og flagger sak for varselvurdering`() {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val meldekortRepo = tac.meldekortRepo
            val lagreMeldekortFraBrukerService = tac.lagreMeldekortFraBrukerService

            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(periode = gyldigPeriode, opprettet = nå(tac.clock)),
                locale = "en",
            )
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

            lagreMeldekortFraBrukerService.lagreMeldekortFraBruker(lagreKommando) shouldBe Unit.right()

            val oppdatertMeldekort = meldekortRepo.hentForMeldekortId(meldekortId = meldekort.id, fnr = meldekort.fnr)
            val forventetMeldekort =
                meldekort.copy(
                    dager = lagreKommando.dager,
                    mottatt = oppdatertMeldekort?.mottatt,
                    locale = "en",
                )

            oppdatertMeldekort shouldBe forventetMeldekort
            tac.sakVarselRepo.hentSakerSomSkalVurdereVarsel().map { it.sakId } shouldBe listOf(meldekort.sakId)
        }
    }

    @Test
    fun `Lagrer innsendt meldekort gjennom hele tjenesten mot ekte database`() {
        withTestApplicationContextAndPostgres(clock = tikkendeKlokke1mars2025()) { tac ->
            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(periode = gyldigPeriode, opprettet = nå(tac.clock)),
                locale = "en",
            )
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

            tac.lagreMeldekortFraBrukerService.lagreMeldekortFraBruker(lagreKommando) shouldBe Unit.right()

            // Verifiserer at den betingede skrivingen faktisk committet og persisterte i ekte Postgres - ikke bare at fake-en oppfører seg riktig.
            val oppdatertMeldekort = tac.meldekortRepo.hentForMeldekortId(meldekort.id, meldekort.fnr)
            oppdatertMeldekort shouldBe meldekort.copy(
                dager = lagreKommando.dager,
                mottatt = oppdatertMeldekort?.mottatt,
                locale = "en",
            )
            oppdatertMeldekort?.mottatt shouldNotBe null
            tac.sakVarselRepo.hentSakerSomSkalVurdereVarsel().map { it.sakId } shouldBe listOf(meldekort.sakId)
        }
    }

    @Test
    fun `Returnerer MeldekortErAlleredeMottatt dersom en samtidig innsending mottok meldekortet under racet`() {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(periode = gyldigPeriode, opprettet = nå(tac.clock)),
            )
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

            // Simulerer at en samtidig request mottok meldekortet mellom lesingen og den betingede skrivingen:
            // skrivingen treffer 0 rader, og den nye lesingen avdekker at meldekortet nå er mottatt.
            val racendeMeldekortRepo = object : MeldekortRepo by tac.meldekortRepo {
                override fun lagreInnsendtMeldekortFraBruker(
                    meldekort: BrukersMeldekort,
                    sessionContext: SessionContext?,
                ): Int {
                    tac.meldekortRepo.lagre(meldekort.copy(mottatt = nå(tac.clock)), sessionContext)
                    return 0
                }
            }
            val service = LagreMeldekortFraBrukerService(
                meldekortRepo = racendeMeldekortRepo,
                meldeperiodeRepo = tac.meldeperiodeRepo,
                sakVarselRepo = tac.sakVarselRepo,
                sessionFactory = tac.sessionFactory,
                clock = tac.clock,
            )

            service.lagreMeldekortFraBruker(lagreKommando) shouldBe
                KunneIkkeLagreMeldekortFraBruker.MeldekortErAlleredeMottatt(meldekort.id).left()

            // Saken skal ikke flagges for varselvurdering når skrivingen ikke traff noen rader.
            tac.sakVarselRepo.hentSakerSomSkalVurdereVarsel() shouldBe emptyList()
        }
    }

    @Test
    fun `Returnerer MeldekortErDeaktivert dersom meldekortet ble deaktivert under racet`() {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(periode = gyldigPeriode, opprettet = nå(tac.clock)),
            )
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

            // Simulerer at meldekortet ble deaktivert mellom lesingen og den betingede skrivingen: skrivingen treffer 0 rader, og den nye lesingen avdekker at meldekortet nå er deaktivert.
            val racendeMeldekortRepo = object : MeldekortRepo by tac.meldekortRepo {
                override fun lagreInnsendtMeldekortFraBruker(
                    meldekort: BrukersMeldekort,
                    sessionContext: SessionContext?,
                ): Int {
                    tac.meldekortRepo.lagre(
                        meldekort.copy(mottatt = null, deaktivert = nå(tac.clock)),
                        sessionContext,
                    )
                    return 0
                }
            }
            val service = LagreMeldekortFraBrukerService(
                meldekortRepo = racendeMeldekortRepo,
                meldeperiodeRepo = tac.meldeperiodeRepo,
                sakVarselRepo = tac.sakVarselRepo,
                sessionFactory = tac.sessionFactory,
                clock = tac.clock,
            )

            service.lagreMeldekortFraBruker(lagreKommando) shouldBe
                KunneIkkeLagreMeldekortFraBruker.MeldekortErDeaktivert(meldekort.id).left()

            tac.sakVarselRepo.hentSakerSomSkalVurdereVarsel() shouldBe emptyList()
        }
    }

    @Test
    fun `Returnerer FantIkkeMeldekort dersom meldekortet forsvant under racet`() {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(periode = gyldigPeriode, opprettet = nå(tac.clock)),
            )
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

            // Simulerer at meldekortet ikke lenger finnes ved den nye lesingen (som skjer i transaksjonen, altså med sessionContext != null): den betingede skrivingen traff 0 rader.
            val racendeMeldekortRepo = object : MeldekortRepo by tac.meldekortRepo {
                override fun lagreInnsendtMeldekortFraBruker(
                    meldekort: BrukersMeldekort,
                    sessionContext: SessionContext?,
                ): Int = 0

                override fun hentForMeldekortId(
                    meldekortId: MeldekortId,
                    fnr: Fnr,
                    sessionContext: SessionContext?,
                ): BrukersMeldekort? =
                    if (sessionContext != null) {
                        null
                    } else {
                        tac.meldekortRepo.hentForMeldekortId(meldekortId, fnr, sessionContext)
                    }
            }
            val service = LagreMeldekortFraBrukerService(
                meldekortRepo = racendeMeldekortRepo,
                meldeperiodeRepo = tac.meldeperiodeRepo,
                sakVarselRepo = tac.sakVarselRepo,
                sessionFactory = tac.sessionFactory,
                clock = tac.clock,
            )

            service.lagreMeldekortFraBruker(lagreKommando) shouldBe
                KunneIkkeLagreMeldekortFraBruker.FantIkkeMeldekort(meldekort.id).left()

            tac.sakVarselRepo.hentSakerSomSkalVurdereVarsel() shouldBe emptyList()
        }
    }

    @Test
    fun `Returnerer UventetFeilVedLagring dersom skrivingen traff 0 rader men meldekortet framstår som åpent`() {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(periode = gyldigPeriode, opprettet = nå(tac.clock)),
            )
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

            // Selvmotsigelse: skrivingen traff 0 rader, men den nye lesingen viser fortsatt et åpent meldekort (KAN_UTFYLLES).
            // Dette skal pakkes inn som en uventet feil (havner i sikkerlogg).
            val racendeMeldekortRepo = object : MeldekortRepo by tac.meldekortRepo {
                override fun lagreInnsendtMeldekortFraBruker(
                    meldekort: BrukersMeldekort,
                    sessionContext: SessionContext?,
                ): Int = 0
            }
            val service = LagreMeldekortFraBrukerService(
                meldekortRepo = racendeMeldekortRepo,
                meldeperiodeRepo = tac.meldeperiodeRepo,
                sakVarselRepo = tac.sakVarselRepo,
                sessionFactory = tac.sessionFactory,
                clock = tac.clock,
            )

            val feil = service.lagreMeldekortFraBruker(lagreKommando)
                .swap().getOrNull()
                .shouldBeInstanceOf<KunneIkkeLagreMeldekortFraBruker.UventetFeilVedLagring>()

            feil.meldekortId shouldBe meldekort.id
            feil.throwable.shouldBeInstanceOf<IllegalStateException>()
            tac.sakVarselRepo.hentSakerSomSkalVurdereVarsel() shouldBe emptyList()
        }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker for periode som ikke er klart til innsending`() {
        withTestApplicationContext { tac ->
            val lagreMeldekortFraBrukerService = tac.lagreMeldekortFraBrukerService

            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(
                    periode = ObjectMother.periode(
                        LocalDate.now(tac.clock),
                    ),
                    opprettet = nå(tac.clock),
                ),
            )
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

            lagreMeldekortFraBrukerService.lagreMeldekortFraBruker(lagreKommando) shouldBe
                KunneIkkeLagreMeldekortFraBruker.MeldekortErIkkeKlartTilInnsending(
                    meldekortId = meldekort.id,
                    status = MeldekortStatus.IKKE_KLAR,
                    kanPrøveIgjenTidspunkt = meldekort.meldeperiode.kanFyllesUtFraOgMed,
                ).left()
        }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker som ikke matcher fnr på meldekortet`() {
        withTestApplicationContext { tac ->
            val lagreMeldekortFraBrukerService = tac.lagreMeldekortFraBrukerService

            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(
                    periode = ObjectMother.periode(LocalDate.of(2025, 1, 1)),
                    opprettet = nå(tac.clock),
                ),
            )
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort, fnr = Fnr.fromString("11111111111"))

            lagreMeldekortFraBrukerService.lagreMeldekortFraBruker(lagreKommando) shouldBe
                KunneIkkeLagreMeldekortFraBruker.FantIkkeMeldekort(meldekort.id).left()
        }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker som allerede er mottatt`() {
        withTestApplicationContext { tac ->
            val lagreMeldekortFraBrukerService = tac.lagreMeldekortFraBrukerService
            val mottatt = nå(tac.clock).plusSeconds(1)

            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(
                    periode = ObjectMother.periode(LocalDate.now(fixedClock)),
                    opprettet = nå(tac.clock),
                ),
                mottatt,
            )
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

            lagreMeldekortFraBrukerService.lagreMeldekortFraBruker(lagreKommando) shouldBe
                KunneIkkeLagreMeldekortFraBruker.MeldekortErAlleredeMottatt(meldekort.id).left()
        }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker når det ikke finnes en meldeperiode for kjeden`() {
        withTestApplicationContext { tac ->
            val lagreMeldekortFraBrukerService = tac.lagreMeldekortFraBrukerService

            // Lagrer meldekortet direkte uten å lagre meldeperioden, slik at oppslaget på siste meldeperiode returnerer null.
            val meldekort = ObjectMother.meldekort(
                meldeperiode = ObjectMother.meldeperiode(periode = gyldigPeriode, opprettet = nå(tac.clock)),
                mottatt = null,
            )
            tac.meldekortRepo.lagre(meldekort)
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

            lagreMeldekortFraBrukerService.lagreMeldekortFraBruker(lagreKommando) shouldBe
                KunneIkkeLagreMeldekortFraBruker.FantIkkeMeldeperiode(
                    meldekortId = meldekort.id,
                    kjedeId = meldekort.meldeperiode.kjedeId,
                ).left()
        }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker når meldeperioden er erstattet av en nyere versjon`() {
        withTestApplicationContext { tac ->
            val lagreMeldekortFraBrukerService = tac.lagreMeldekortFraBrukerService

            val meldeperiodeV1 = ObjectMother.meldeperiode(periode = gyldigPeriode, opprettet = nå(tac.clock))
            val meldekort = tac.lagMeldekort(meldeperiodeV1)

            val meldeperiodeV2 = ObjectMother.meldeperiode(
                periode = gyldigPeriode,
                fnr = meldeperiodeV1.fnr,
                sakId = meldeperiodeV1.sakId,
                saksnummer = meldeperiodeV1.saksnummer,
                versjon = meldeperiodeV1.versjon + 1,
                opprettet = nå(tac.clock),
            )
            tac.lagreMeldeperiode(meldeperiodeV2)

            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

            lagreMeldekortFraBrukerService.lagreMeldekortFraBruker(lagreKommando) shouldBe
                KunneIkkeLagreMeldekortFraBruker.MeldekortetsMeldeperiodeErErstattet(
                    meldekortId = meldekort.id,
                    meldekortetsMeldeperiodeId = meldeperiodeV1.id,
                    sisteMeldeperiodeId = meldeperiodeV2.id,
                ).left()
        }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker som er deaktivert`() {
        withTestApplicationContext { tac ->
            val lagreMeldekortFraBrukerService = tac.lagreMeldekortFraBrukerService

            // Meldekortet er deaktivert, men meldeperioden er fortsatt den siste (passerer erstattet-sjekken), slik at vi havner i DEAKTIVERT-grenen i statushåndteringen.
            val meldeperiode = ObjectMother.meldeperiode(periode = gyldigPeriode, opprettet = nå(tac.clock))
            tac.lagreMeldeperiode(meldeperiode)
            val meldekort = ObjectMother.meldekort(
                meldeperiode = meldeperiode,
                mottatt = null,
                deaktivert = nå(tac.clock),
            )
            tac.meldekortRepo.lagre(meldekort)
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

            lagreMeldekortFraBrukerService.lagreMeldekortFraBruker(lagreKommando) shouldBe
                KunneIkkeLagreMeldekortFraBruker.MeldekortErDeaktivert(meldekort.id).left()
        }
    }

    @Test
    fun `Uventet feil under utfylling pakkes inn som UventetFeilVedLagring med throwable`() {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val lagreMeldekortFraBrukerService = tac.lagreMeldekortFraBrukerService

            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(periode = gyldigPeriode, opprettet = nå(tac.clock)),
            )
            // Tomme dager bryter en domeneinvariant i utfyllingen og kaster en exception som skal fanges.
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort).copy(dager = emptyList())

            val feil = lagreMeldekortFraBrukerService.lagreMeldekortFraBruker(lagreKommando)
                .swap().getOrNull()
                .shouldBeInstanceOf<KunneIkkeLagreMeldekortFraBruker.UventetFeilVedLagring>()

            feil.meldekortId shouldBe meldekort.id
            feil.throwable.shouldBeInstanceOf<RuntimeException>()
        }
    }
}
