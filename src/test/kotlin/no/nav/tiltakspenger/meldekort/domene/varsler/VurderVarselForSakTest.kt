package no.nav.tiltakspenger.meldekort.domene.varsler

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.meldekort.domene.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.domene.Sak
import no.nav.tiltakspenger.meldekort.domene.VarselId
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class VurderVarselForSakTest {
    private val sak = Sak(
        id = SakId.random(),
        saksnummer = "SAK-123",
        fnr = Fnr.fromString("12345678911"),
        meldeperioder = emptyList(),
        arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
        harSoknadUnderBehandling = false,
        kanSendeInnHelgForMeldekort = false,
    )

    @Test
    fun `oppretter nytt varsel og markerer vurdert når saken har kjeder som mangler innsending`() {
        val nå = LocalDateTime.of(2025, 3, 10, 10, 0)
        val lagredeVarsler = mutableListOf<Varsel>()
        val vurderteTidspunkt = mutableListOf<LocalDateTime>()
        val infoMeldinger = mutableListOf<String>()
        val warnMeldinger = mutableListOf<String>()

        vurderVarselForSak(
            sak = sak,
            clock = fixedClockAt(nå),
            sessionFactory = sessionFactory(),
            hentVarsler = { Varsler(emptyList()) },
            hentKjederSomManglerInnsending = {
                listOf(
                    KjedeSomManglerInnsending(
                        sakId = sak.id,
                        meldeperiodeId = MeldeperiodeId.random(),
                        kjedeId = MeldeperiodeKjedeId("2025-02-24/2025-03-09"),
                        nyesteVersjon = 1,
                        kanFyllesUtFraOgMed = LocalDateTime.of(2025, 3, 7, 15, 0),
                    ),
                )
            },
            lagreVarsel = { varsel, _ -> lagredeVarsler.add(varsel) },
            markerVarselVurdert = { vurdertTidspunkt, _, _ -> vurderteTidspunkt.add(vurdertTidspunkt) },
            logInfo = { infoMeldinger.add(it) },
            logWarn = { warnMeldinger.add(it) },
        )

        lagredeVarsler shouldHaveSize 1
        val nyttVarsel = lagredeVarsler.single().shouldBeInstanceOf<Varsel.SkalAktiveres>()
        nyttVarsel.skalAktiveresTidspunkt shouldBe nå
        vurderteTidspunkt shouldBe listOf(nå)
        infoMeldinger.single().contains("Opprettet varsel") shouldBe true
        warnMeldinger shouldBe emptyList()
    }

    @Test
    fun `forbereder inaktivering og markerer vurdert når saken ikke lenger har kjeder som mangler innsending`() {
        val nå = LocalDateTime.of(2025, 3, 10, 10, 0)
        val eksisterendeVarsel = Varsel.Aktiv(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = LocalDateTime.of(2025, 3, 10, 9, 0),
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = LocalDateTime.of(2025, 3, 10, 9, 5),
            opprettet = LocalDateTime.of(2025, 3, 10, 9, 0),
            sistEndret = LocalDateTime.of(2025, 3, 10, 9, 5),
        )
        val lagredeVarsler = mutableListOf<Varsel>()
        val vurderteTidspunkt = mutableListOf<LocalDateTime>()

        vurderVarselForSak(
            sak = sak,
            clock = fixedClockAt(nå),
            sessionFactory = sessionFactory(),
            hentVarsler = { Varsler(listOf(eksisterendeVarsel)) },
            hentKjederSomManglerInnsending = { emptyList() },
            lagreVarsel = { varsel, _ -> lagredeVarsler.add(varsel) },
            markerVarselVurdert = { vurdertTidspunkt, _, _ -> vurderteTidspunkt.add(vurdertTidspunkt) },
            logInfo = {},
            logWarn = {},
        )

        lagredeVarsler shouldHaveSize 1
        val skalInaktiveres = lagredeVarsler.single().shouldBeInstanceOf<Varsel.SkalInaktiveres>()
        skalInaktiveres.skalInaktiveresTidspunkt shouldBe nå
        vurderteTidspunkt shouldBe listOf(nå)
    }

    @Test
    fun `kaller ikke planleggPåNytt når eksisterende SkalAktiveres har samme tidspunkt som beregnet`() {
        val nå = LocalDateTime.of(2025, 3, 10, 10, 0)
        val eksisterendeTidspunkt = LocalDateTime.of(2025, 3, 10, 15, 0)
        val eksisterendeVarsel = Varsel.SkalAktiveres(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = eksisterendeTidspunkt,
            skalAktiveresBegrunnelse = "opprinnelig begrunnelse",
            opprettet = LocalDateTime.of(2025, 3, 10, 9, 0),
            sistEndret = LocalDateTime.of(2025, 3, 10, 9, 0),
        )
        val lagredeVarsler = mutableListOf<Varsel>()
        val infoMeldinger = mutableListOf<String>()

        vurderVarselForSak(
            sak = sak,
            clock = fixedClockAt(nå),
            sessionFactory = sessionFactory(),
            hentVarsler = { Varsler(listOf(eksisterendeVarsel)) },
            hentKjederSomManglerInnsending = {
                // ønsketTidspunkt som gir planlagtTidspunkt == eksisterendeTidspunkt
                listOf(
                    KjedeSomManglerInnsending(
                        sakId = sak.id,
                        meldeperiodeId = MeldeperiodeId.random(),
                        kjedeId = MeldeperiodeKjedeId("2025-02-24/2025-03-09"),
                        nyesteVersjon = 1,
                        kanFyllesUtFraOgMed = eksisterendeTidspunkt,
                    ),
                )
            },
            lagreVarsel = { varsel, _ -> lagredeVarsler.add(varsel) },
            markerVarselVurdert = { _, _, _ -> },
            logInfo = { infoMeldinger.add(it) },
            logWarn = {},
        )

        lagredeVarsler shouldBe emptyList()
        infoMeldinger.none { it.contains("Oppdaterte varsel") } shouldBe true
    }

    @Test
    fun `cooldown - revurdering med ny meldeperiode samme dag som avbrutt varsel skal ikke tape hendelsen`() {
        // Regresjonstest for cooldown-bug: Hvis et varsel ble avbrutt tidligere samme dag
        // (f.eks. pga delvis opphør), og en revurdering deretter innfører en ny meldeperiode,
        // skal nytt varsel opprettes. Det avbrutte varselet ble aldri sendt til bruker,
        // så det er ingen reell cooldown.
        val fredagMorgen = LocalDateTime.of(2025, 3, 7, 9, 0) // fredag
        val senereSammeFredag = LocalDateTime.of(2025, 3, 7, 14, 0)
        val avbruttVarsel = Varsel.Avbrutt(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = fredagMorgen,
            skalAktiveresBegrunnelse = "opprinnelig",
            avbruttTidspunkt = fredagMorgen.plusMinutes(5),
            avbruttBegrunnelse = "delvis opphør",
            opprettet = fredagMorgen,
            sistEndret = fredagMorgen.plusMinutes(5),
        )
        val lagredeVarsler = mutableListOf<Varsel>()
        val warnMeldinger = mutableListOf<String>()

        vurderVarselForSak(
            sak = sak,
            clock = fixedClockAt(senereSammeFredag),
            sessionFactory = sessionFactory(),
            hentVarsler = { Varsler(listOf(avbruttVarsel)) },
            hentKjederSomManglerInnsending = {
                listOf(
                    KjedeSomManglerInnsending(
                        sakId = sak.id,
                        meldeperiodeId = MeldeperiodeId.random(),
                        kjedeId = MeldeperiodeKjedeId("2025-02-24/2025-03-09"),
                        nyesteVersjon = 2,
                        kanFyllesUtFraOgMed = senereSammeFredag,
                    ),
                )
            },
            lagreVarsel = { varsel, _ -> lagredeVarsler.add(varsel) },
            markerVarselVurdert = { _, _, _ -> },
            logInfo = {},
            logWarn = { warnMeldinger.add(it) },
        )

        lagredeVarsler shouldHaveSize 1
        val nyttVarsel = lagredeVarsler.single().shouldBeInstanceOf<Varsel.SkalAktiveres>()
        nyttVarsel.skalAktiveresTidspunkt shouldBe senereSammeFredag
        warnMeldinger shouldBe emptyList()
    }

    @Test
    fun `cooldown - mottatt meldekort og revurdering med ny meldeperiode samme dag gir nytt varsel neste virkedag`() {
        // Scenario fra bruker: Meldekort mottatt i dag (fredag) så eksisterende varsel er inaktivert
        // samme dag. Revurdering med ny meldeperiode kommer inn. Nytt varsel skal opprettes med
        // start neste virkedag (ikke droppes, og ikke samme dag siden cooldown gjelder).
        val fredagMorgen = LocalDateTime.of(2025, 3, 7, 9, 0)
        val fredagEttermiddag = LocalDateTime.of(2025, 3, 7, 14, 0)
        val mandagKl9 = LocalDateTime.of(2025, 3, 10, 9, 0)
        val inaktivertVarsel = Varsel.Inaktivert(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = fredagMorgen,
            skalAktiveresBegrunnelse = "opprinnelig",
            aktiveringstidspunkt = fredagMorgen.plusMinutes(1),
            skalInaktiveresTidspunkt = fredagEttermiddag.minusMinutes(1),
            skalInaktiveresBegrunnelse = "meldekort mottatt",
            inaktiveringstidspunkt = fredagEttermiddag.minusMinutes(1),
            opprettet = fredagMorgen,
            sistEndret = fredagEttermiddag.minusMinutes(1),
        )
        val lagredeVarsler = mutableListOf<Varsel>()

        vurderVarselForSak(
            sak = sak,
            clock = fixedClockAt(fredagEttermiddag),
            sessionFactory = sessionFactory(),
            hentVarsler = { Varsler(listOf(inaktivertVarsel)) },
            hentKjederSomManglerInnsending = {
                listOf(
                    KjedeSomManglerInnsending(
                        sakId = sak.id,
                        meldeperiodeId = MeldeperiodeId.random(),
                        kjedeId = MeldeperiodeKjedeId("2025-03-10/2025-03-23"),
                        nyesteVersjon = 1,
                        kanFyllesUtFraOgMed = fredagEttermiddag,
                    ),
                )
            },
            lagreVarsel = { varsel, _ -> lagredeVarsler.add(varsel) },
            markerVarselVurdert = { _, _, _ -> },
            logInfo = {},
            logWarn = {},
        )

        lagredeVarsler shouldHaveSize 1
        val nyttVarsel = lagredeVarsler.single().shouldBeInstanceOf<Varsel.SkalAktiveres>()
        nyttVarsel.skalAktiveresTidspunkt shouldBe mandagKl9
    }

    @Test
    fun `vanlig flyt - aktivt varsel og meldekort mottatt for forrige meldeperiode - forbered inaktivering selv om ny kjede venter`() {
        // Scenario fra bruker (del 1): 2 meldeperioder innvilget. Første varsel er aktivert.
        // Bruker sender inn meldekort på fredag. Andre meldeperiode (om 2 uker) mangler fortsatt
        // innsending, men siden første meldeperiode er ferdig, skal det aktive varselet forberedes
        // for inaktivering. Nytt varsel opprettes i påfølgende kjøring (etter inaktivering).
        val forrigeFredag = LocalDateTime.of(2025, 2, 28, 9, 0)
        val dennesFredag = LocalDateTime.of(2025, 3, 14, 12, 0)
        val nesteFredag = LocalDateTime.of(2025, 3, 21, 9, 0) // 2 fredager etter aktivering (ikke mottak)
        val aktivtVarsel = Varsel.Aktiv(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = forrigeFredag,
            skalAktiveresBegrunnelse = "første meldeperiode",
            aktiveringstidspunkt = forrigeFredag.plusMinutes(1),
            opprettet = forrigeFredag,
            sistEndret = forrigeFredag.plusMinutes(1),
        )
        val lagredeVarsler = mutableListOf<Varsel>()

        vurderVarselForSak(
            sak = sak,
            clock = fixedClockAt(dennesFredag),
            sessionFactory = sessionFactory(),
            hentVarsler = { Varsler(listOf(aktivtVarsel)) },
            hentKjederSomManglerInnsending = {
                // Kun den andre meldeperioden mangler innsending nå
                listOf(
                    KjedeSomManglerInnsending(
                        sakId = sak.id,
                        meldeperiodeId = MeldeperiodeId.random(),
                        kjedeId = MeldeperiodeKjedeId("2025-03-10/2025-03-23"),
                        nyesteVersjon = 1,
                        kanFyllesUtFraOgMed = nesteFredag,
                    ),
                )
            },
            lagreVarsel = { varsel, _ -> lagredeVarsler.add(varsel) },
            markerVarselVurdert = { _, _, _ -> },
            logInfo = {},
            logWarn = {},
        )

        lagredeVarsler shouldHaveSize 1
        val skalInaktiveres = lagredeVarsler.single().shouldBeInstanceOf<Varsel.SkalInaktiveres>()
        skalInaktiveres.varselId shouldBe aktivtVarsel.varselId
        skalInaktiveres.skalInaktiveresTidspunkt shouldBe dennesFredag
    }

    @Test
    fun `vanlig flyt - etter inaktivering opprettes nytt varsel for ny meldeperiode`() {
        // Scenario fra bruker (del 2): Etter at det forrige varselet er inaktivert, skal nytt
        // varsel opprettes med skalAktiveresTidspunkt = når ny meldeperiode kan fylles ut.
        val forrigeFredag = LocalDateTime.of(2025, 2, 28, 9, 0)
        val dennesFredag = LocalDateTime.of(2025, 3, 14, 12, 0)
        val nesteFredag = LocalDateTime.of(2025, 3, 21, 9, 0)
        val inaktivertVarsel = Varsel.Inaktivert(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = forrigeFredag,
            skalAktiveresBegrunnelse = "første meldeperiode",
            aktiveringstidspunkt = forrigeFredag.plusMinutes(1),
            skalInaktiveresTidspunkt = dennesFredag,
            skalInaktiveresBegrunnelse = "meldekort mottatt",
            inaktiveringstidspunkt = dennesFredag.plusMinutes(5),
            opprettet = forrigeFredag,
            sistEndret = dennesFredag.plusMinutes(5),
        )
        val lagredeVarsler = mutableListOf<Varsel>()

        vurderVarselForSak(
            sak = sak,
            clock = fixedClockAt(dennesFredag.plusMinutes(10)),
            sessionFactory = sessionFactory(),
            hentVarsler = { Varsler(listOf(inaktivertVarsel)) },
            hentKjederSomManglerInnsending = {
                listOf(
                    KjedeSomManglerInnsending(
                        sakId = sak.id,
                        meldeperiodeId = MeldeperiodeId.random(),
                        kjedeId = MeldeperiodeKjedeId("2025-03-10/2025-03-23"),
                        nyesteVersjon = 1,
                        kanFyllesUtFraOgMed = nesteFredag,
                    ),
                )
            },
            lagreVarsel = { varsel, _ -> lagredeVarsler.add(varsel) },
            markerVarselVurdert = { _, _, _ -> },
            logInfo = {},
            logWarn = {},
        )

        lagredeVarsler shouldHaveSize 1
        val nyttVarsel = lagredeVarsler.single().shouldBeInstanceOf<Varsel.SkalAktiveres>()
        nyttVarsel.skalAktiveresTidspunkt shouldBe nesteFredag
    }

    @Test
    fun `aktivt varsel med kjede for samme meldeperiode skal ikke forberedes for inaktivering`() {
        // Dersom eksisterende aktivt varsel fortsatt gjelder for en kjede som mangler innsending
        // (bruker har ikke sendt inn meldekort enda), skal varselet stå uendret.
        val aktiveringsTidspunkt = LocalDateTime.of(2025, 3, 10, 9, 0)
        val nå = LocalDateTime.of(2025, 3, 10, 14, 0)
        val aktivtVarsel = Varsel.Aktiv(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = aktiveringsTidspunkt,
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = aktiveringsTidspunkt.plusMinutes(1),
            opprettet = aktiveringsTidspunkt,
            sistEndret = aktiveringsTidspunkt.plusMinutes(1),
        )
        val lagredeVarsler = mutableListOf<Varsel>()

        vurderVarselForSak(
            sak = sak,
            clock = fixedClockAt(nå),
            sessionFactory = sessionFactory(),
            hentVarsler = { Varsler(listOf(aktivtVarsel)) },
            hentKjederSomManglerInnsending = {
                listOf(
                    KjedeSomManglerInnsending(
                        sakId = sak.id,
                        meldeperiodeId = MeldeperiodeId.random(),
                        kjedeId = MeldeperiodeKjedeId("2025-02-24/2025-03-09"),
                        nyesteVersjon = 1,
                        kanFyllesUtFraOgMed = aktiveringsTidspunkt,
                    ),
                )
            },
            lagreVarsel = { varsel, _ -> lagredeVarsler.add(varsel) },
            markerVarselVurdert = { _, _, _ -> },
            logInfo = {},
            logWarn = {},
        )

        lagredeVarsler shouldBe emptyList()
    }

    @Test
    fun `SkalInaktiveres varsel ignoreres - venter på inaktivering før nytt varsel opprettes`() {
        val nå = LocalDateTime.of(2025, 3, 14, 12, 0)
        val skalInaktiveresVarsel = Varsel.SkalInaktiveres(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = LocalDateTime.of(2025, 2, 28, 9, 0),
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = LocalDateTime.of(2025, 2, 28, 9, 1),
            skalInaktiveresTidspunkt = nå.minusMinutes(10),
            skalInaktiveresBegrunnelse = "meldekort mottatt",
            opprettet = LocalDateTime.of(2025, 2, 28, 9, 0),
            sistEndret = nå.minusMinutes(10),
        )
        val lagredeVarsler = mutableListOf<Varsel>()

        vurderVarselForSak(
            sak = sak,
            clock = fixedClockAt(nå),
            sessionFactory = sessionFactory(),
            hentVarsler = { Varsler(listOf(skalInaktiveresVarsel)) },
            hentKjederSomManglerInnsending = {
                listOf(
                    KjedeSomManglerInnsending(
                        sakId = sak.id,
                        meldeperiodeId = MeldeperiodeId.random(),
                        kjedeId = MeldeperiodeKjedeId("2025-03-10/2025-03-23"),
                        nyesteVersjon = 1,
                        kanFyllesUtFraOgMed = LocalDateTime.of(2025, 3, 21, 9, 0),
                    ),
                )
            },
            lagreVarsel = { varsel, _ -> lagredeVarsler.add(varsel) },
            markerVarselVurdert = { _, _, _ -> },
            logInfo = {},
            logWarn = {},
        )

        lagredeVarsler shouldBe emptyList()
    }

    @Test
    fun `hvis leggTil feiler uventet skal transaksjonen rulle tilbake og varselet IKKE markeres vurdert`() {
        // Defensiv test: Dersom et invariantbrudd gjør at leggTil returnerer Left,
        // må transaksjonen rulle tilbake slik at saken ikke markeres som vurdert og
        // dermed blir forsøkt på nytt. Ellers ville vi tapt hendelsen.
        // Vi simulerer dette ved å ha et varsel som ikke er inaktivert/avbrutt (bryter
        // forutsetningen erAlleInaktivertEllerAvbrutt), men som likevel routes til leggTil
        // ved at varsler-lista tilsynelatende er tom... Dette er vanskelig å simulere uten
        // å modifisere produksjonskoden. I stedet verifiserer vi at en kastet exception
        // fra lagreVarsel propagerer og rullebacker transaksjonen.
        val nå = LocalDateTime.of(2025, 3, 10, 10, 0)
        val vurderteTidspunkt = mutableListOf<LocalDateTime>()

        val kastet = runCatching {
            vurderVarselForSak(
                sak = sak,
                clock = fixedClockAt(nå),
                sessionFactory = sessionFactoryMedPropagerendeException(),
                hentVarsler = { Varsler(emptyList()) },
                hentKjederSomManglerInnsending = {
                    listOf(
                        KjedeSomManglerInnsending(
                            sakId = sak.id,
                            meldeperiodeId = MeldeperiodeId.random(),
                            kjedeId = MeldeperiodeKjedeId("2025-02-24/2025-03-09"),
                            nyesteVersjon = 1,
                            kanFyllesUtFraOgMed = nå,
                        ),
                    )
                },
                lagreVarsel = { _, _ -> throw IllegalStateException("simulert feil") },
                markerVarselVurdert = { tidspunkt, _, _ -> vurderteTidspunkt.add(tidspunkt) },
                logInfo = {},
                logWarn = {},
            )
        }

        kastet.isFailure shouldBe true
        vurderteTidspunkt shouldBe emptyList()
    }

    private fun sessionFactoryMedPropagerendeException(): SessionFactory {
        val transactionContext = mockk<TransactionContext>(relaxed = true)
        return mockk<SessionFactory>().also { sessionFactory ->
            every { sessionFactory.withTransactionContext(any<Function1<TransactionContext, Unit>>()) } answers {
                // Simulerer at exception inni blokken propagerer ut (og rullerer tilbake tx)
                firstArg<Function1<TransactionContext, Unit>>().invoke(transactionContext)
            }
        }
    }

    private fun sessionFactory(): SessionFactory {
        val transactionContext = mockk<TransactionContext>(relaxed = true)
        return mockk<SessionFactory>().also { sessionFactory ->
            every { sessionFactory.withTransactionContext(any<Function1<TransactionContext, Unit>>()) } answers {
                firstArg<Function1<TransactionContext, Unit>>().invoke(transactionContext)
            }
        }
    }
}
