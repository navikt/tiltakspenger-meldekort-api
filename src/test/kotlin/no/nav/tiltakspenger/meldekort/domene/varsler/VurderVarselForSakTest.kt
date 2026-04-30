package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.left
import arrow.core.right
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

        vurderVarselForSak(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
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
        )

        lagredeVarsler shouldHaveSize 1
        val nyttVarsel = lagredeVarsler.single().shouldBeInstanceOf<Varsel.SkalAktiveres>()
        // skalAktiveresTidspunkt = kjedens kanFyllesUtFraOgMed (kan være i fortiden – det er OK,
        // brukeren kunne ha sendt inn meldekortet fra og med dette tidspunktet).
        nyttVarsel.skalAktiveresTidspunkt shouldBe LocalDateTime.of(2025, 3, 7, 15, 0)
        vurderteTidspunkt shouldBe listOf(nå)
    }

    @Test
    fun `forbereder inaktivering og markerer vurdert når saken ikke lenger har kjeder som mangler innsending`() {
        val nå = LocalDateTime.of(2025, 3, 10, 10, 0)
        val eksisterendeVarsel = aktivVarsel(
            skalAktiveresTidspunkt = LocalDateTime.of(2025, 3, 10, 9, 0),
            aktiveringstidspunkt = LocalDateTime.of(2025, 3, 10, 9, 5),
        )
        val lagredeVarsler = mutableListOf<Varsel>()
        val vurderteTidspunkt = mutableListOf<LocalDateTime>()

        vurderVarselForSak(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            clock = fixedClockAt(nå),
            sessionFactory = sessionFactory(),
            hentVarsler = { Varsler(listOf(eksisterendeVarsel)) },
            hentKjederSomManglerInnsending = { emptyList() },
            lagreVarsel = { varsel, _ -> lagredeVarsler.add(varsel) },
            markerVarselVurdert = { vurdertTidspunkt, _, _ -> vurderteTidspunkt.add(vurdertTidspunkt) },
        )

        lagredeVarsler shouldHaveSize 1
        val skalInaktiveres = lagredeVarsler.single().shouldBeInstanceOf<Varsel.SkalInaktiveres>()
        skalInaktiveres.skalInaktiveresTidspunkt shouldBe nå
        vurderteTidspunkt shouldBe listOf(nå)
    }

    @Test
    fun `SkalAktiveres med ingen kjeder forbereder inaktivering direkte`() {
        // Når meldeperioden opphører før varselet er aktivert må vi gå rett fra SkalAktiveres
        // til SkalInaktiveres (sikkerhetsnett: aktiveringen kan ha blitt publisert på Kafka uten
        // at vi rakk å persistere overgangen til Aktiv).
        val nå = LocalDateTime.of(2025, 3, 10, 10, 0)
        val eksisterendeVarsel = Varsel.SkalAktiveres(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = LocalDateTime.of(2025, 3, 11, 9, 0),
            skalAktiveresEksterntTidspunkt = LocalDateTime.of(2025, 3, 11, 9, 0),
            skalAktiveresBegrunnelse = "test",
            opprettet = LocalDateTime.of(2025, 3, 10, 9, 0),
            sistEndret = LocalDateTime.of(2025, 3, 10, 9, 0),
        )
        val lagredeVarsler = mutableListOf<Varsel>()

        vurderVarselForSak(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            clock = fixedClockAt(nå),
            sessionFactory = sessionFactory(),
            hentVarsler = { Varsler(listOf(eksisterendeVarsel)) },
            hentKjederSomManglerInnsending = { emptyList() },
            lagreVarsel = { varsel, _ -> lagredeVarsler.add(varsel) },
            markerVarselVurdert = { _, _, _ -> },
        )

        val skalInaktiveres = lagredeVarsler.single().shouldBeInstanceOf<Varsel.SkalInaktiveres>()
        skalInaktiveres.varselId shouldBe eksisterendeVarsel.varselId
        skalInaktiveres.aktiveringstidspunkt shouldBe null
        skalInaktiveres.skalInaktiveresTidspunkt shouldBe nå
    }

    @Test
    fun `SkalAktiveres med oppdatert tidspunkt for samme meldeperiode erstattes`() {
        val nå = LocalDateTime.of(2025, 3, 10, 10, 0)
        val eksisterendeTidspunkt = LocalDateTime.of(2025, 3, 10, 15, 0)
        val eksisterendeVarsel = Varsel.SkalAktiveres(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = eksisterendeTidspunkt,
            skalAktiveresEksterntTidspunkt = eksisterendeTidspunkt,
            skalAktiveresBegrunnelse = "opprinnelig begrunnelse",
            opprettet = LocalDateTime.of(2025, 3, 10, 9, 0),
            sistEndret = LocalDateTime.of(2025, 3, 10, 9, 0),
        )
        val lagredeVarsler = mutableListOf<Varsel>()

        val resultat = vurderVarselForSak(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            clock = fixedClockAt(nå),
            sessionFactory = sessionFactory(),
            hentVarsler = { Varsler(listOf(eksisterendeVarsel)) },
            hentKjederSomManglerInnsending = {
                // Kjede med et annet ønsket tidspunkt – VurderVarsel skal likevel ikke røre SkalAktiveres.
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
            markerVarselVurdert = { _, _, _ -> },
        )

        resultat shouldBe Unit.right()
        lagredeVarsler shouldHaveSize 2
        lagredeVarsler.filterIsInstance<Varsel.SkalInaktiveres>().single().varselId shouldBe eksisterendeVarsel.varselId
        lagredeVarsler.filterIsInstance<Varsel.SkalAktiveres>().single().skalAktiveresTidspunkt shouldBe LocalDateTime.of(2025, 3, 7, 15, 0)
    }

    @Test
    fun `vanlig flyt - aktivt varsel og meldekort mottatt for forrige meldeperiode - forbered inaktivering og opprett nytt i samme runde`() {
        // 2 meldeperioder. Første varsel aktivert. Bruker sender inn meldekort; andre
        // meldeperiode (nesteFredag) mangler fortsatt innsending. I samme transaksjon skal
        // det aktive varselet forberedes for inaktivering OG et nytt SkalAktiveres opprettes.
        val forrigeFredag = LocalDateTime.of(2025, 2, 28, 9, 0)
        val dennesFredag = LocalDateTime.of(2025, 3, 14, 12, 0)
        val nesteFredag = LocalDateTime.of(2025, 3, 21, 9, 0)
        val aktivtVarsel = aktivVarsel(
            skalAktiveresTidspunkt = forrigeFredag,
            aktiveringstidspunkt = forrigeFredag.plusMinutes(1),
            opprettet = forrigeFredag,
        )
        val lagredeVarsler = mutableListOf<Varsel>()

        vurderVarselForSak(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            clock = fixedClockAt(dennesFredag),
            sessionFactory = sessionFactory(),
            hentVarsler = { Varsler(listOf(aktivtVarsel)) },
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
        )

        lagredeVarsler shouldHaveSize 2
        val skalInaktiveres = lagredeVarsler.filterIsInstance<Varsel.SkalInaktiveres>().single()
        skalInaktiveres.varselId shouldBe aktivtVarsel.varselId
        skalInaktiveres.skalInaktiveresTidspunkt shouldBe dennesFredag

        val nyttVarsel = lagredeVarsler.filterIsInstance<Varsel.SkalAktiveres>().single()
        nyttVarsel.skalAktiveresTidspunkt shouldBe nesteFredag
    }

    @Test
    fun `aktivt varsel med kjede for samme meldeperiode skal ikke forberedes for inaktivering`() {
        val aktiveringsTidspunkt = LocalDateTime.of(2025, 3, 10, 9, 0)
        val nå = LocalDateTime.of(2025, 3, 10, 14, 0)
        val aktivtVarsel = aktivVarsel(
            skalAktiveresTidspunkt = aktiveringsTidspunkt,
            aktiveringstidspunkt = aktiveringsTidspunkt.plusMinutes(1),
            opprettet = aktiveringsTidspunkt,
        )
        val lagredeVarsler = mutableListOf<Varsel>()

        val resultat = vurderVarselForSak(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
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
        )

        resultat shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErInnenforEnTimeAktivtVarsel.left()
        lagredeVarsler shouldBe emptyList()
    }

    @Test
    fun `aktivt varsel med senere tidspunkt samme dag kan ikke erstattes når cooldown ville blitt brutt`() {
        val aktiveringsTidspunkt = LocalDateTime.of(2025, 3, 10, 9, 0)
        val nå = LocalDateTime.of(2025, 3, 10, 10, 0)
        val aktivtVarsel = aktivVarsel(
            skalAktiveresTidspunkt = aktiveringsTidspunkt,
            aktiveringstidspunkt = aktiveringsTidspunkt.plusMinutes(1),
            opprettet = aktiveringsTidspunkt,
        )
        val lagredeVarsler = mutableListOf<Varsel>()

        val resultat = vurderVarselForSak(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
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
                        kanFyllesUtFraOgMed = LocalDateTime.of(2025, 3, 10, 12, 1),
                    ),
                )
            },
            lagreVarsel = { varsel, _ -> lagredeVarsler.add(varsel) },
            markerVarselVurdert = { _, _, _ -> },
        )

        resultat shouldBe VurderVarselUtfall.KanIkkeErstattePåGrunnAvCooldown.left()
        lagredeVarsler shouldBe emptyList()
    }

    @Test
    fun `aktivt varsel med pågående inaktivering venter med å opprette nytt varsel`() {
        val aktiveringsTidspunkt = LocalDateTime.of(2025, 3, 10, 9, 0)
        val nå = LocalDateTime.of(2025, 3, 10, 10, 0)
        val aktivtVarsel = aktivVarsel(
            skalAktiveresTidspunkt = aktiveringsTidspunkt,
            aktiveringstidspunkt = aktiveringsTidspunkt.plusMinutes(1),
            opprettet = aktiveringsTidspunkt,
        )
        val pågåendeInaktivering = Varsel.SkalInaktiveres(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = LocalDateTime.of(2025, 3, 9, 9, 0),
            skalAktiveresEksterntTidspunkt = LocalDateTime.of(2025, 3, 9, 9, 0),
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = LocalDateTime.of(2025, 3, 9, 9, 1),
            eksternAktiveringstidspunkt = LocalDateTime.of(2025, 3, 9, 9, 1),
            skalInaktiveresTidspunkt = nå.minusMinutes(5),
            skalInaktiveresBegrunnelse = "test",
            opprettet = LocalDateTime.of(2025, 3, 9, 9, 0),
            sistEndret = nå.minusMinutes(5),
        )
        val lagredeVarsler = mutableListOf<Varsel>()

        val resultat = vurderVarselForSak(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            clock = fixedClockAt(nå),
            sessionFactory = sessionFactory(),
            hentVarsler = { Varsler(listOf(pågåendeInaktivering, aktivtVarsel)) },
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
        )

        resultat shouldBe VurderVarselUtfall.HarPågåendeInaktivering.left()
        lagredeVarsler shouldBe emptyList()
    }

    @Test
    fun `SkalInaktiveres alene ignoreres - venter på inaktivering før nytt varsel opprettes`() {
        // Hvis sak har SkalInaktiveres men ikke noe pågående Aktiv/SkalAktiveres,
        // trenger vi ikke gjøre noe. Et evt. nytt varsel opprettes i neste runde
        // (etter at InaktiverVarslerService har kjørt).
        //
        // Merk: Nå aksepterer Varsler-invarianten SkalInaktiveres sammen med en ny
        // SkalAktiveres. Men VurderVarsel sin pågåendeOppretting == null her, og
        // opprett-grenen vil opprette nytt SkalAktiveres. Vi tester derfor at dette skjer.
        val nå = LocalDateTime.of(2025, 3, 14, 12, 0)
        val skalInaktiveresVarsel = Varsel.SkalInaktiveres(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = LocalDateTime.of(2025, 2, 28, 9, 0),
            skalAktiveresEksterntTidspunkt = LocalDateTime.of(2025, 2, 28, 9, 0),
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = LocalDateTime.of(2025, 2, 28, 9, 1),
            eksternAktiveringstidspunkt = LocalDateTime.of(2025, 2, 28, 9, 1),
            skalInaktiveresTidspunkt = nå.minusMinutes(10),
            skalInaktiveresBegrunnelse = "meldekort mottatt",
            opprettet = LocalDateTime.of(2025, 2, 28, 9, 0),
            sistEndret = nå.minusMinutes(10),
        )
        val lagredeVarsler = mutableListOf<Varsel>()

        vurderVarselForSak(
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            fnr = sak.fnr,
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
        )

        // Nytt SkalAktiveres skal opprettes nå som invarianten tillater sameksistens.
        lagredeVarsler shouldHaveSize 1
        lagredeVarsler.single().shouldBeInstanceOf<Varsel.SkalAktiveres>()
    }

    @Test
    fun `hvis leggTil feiler uventet skal transaksjonen rulle tilbake og varselet IKKE markeres vurdert`() {
        val nå = LocalDateTime.of(2025, 3, 10, 10, 0)
        val vurderteTidspunkt = mutableListOf<LocalDateTime>()

        val kastet = runCatching {
            vurderVarselForSak(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                fnr = sak.fnr,
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
            )
        }

        kastet.isFailure shouldBe true
        vurderteTidspunkt shouldBe emptyList()
    }

    private fun aktivVarsel(
        skalAktiveresTidspunkt: LocalDateTime,
        aktiveringstidspunkt: LocalDateTime = skalAktiveresTidspunkt,
        opprettet: LocalDateTime = skalAktiveresTidspunkt,
    ): Varsel.Aktiv = Varsel.Aktiv(
        sakId = sak.id,
        saksnummer = sak.saksnummer,
        fnr = sak.fnr,
        varselId = VarselId.random(),
        skalAktiveresTidspunkt = skalAktiveresTidspunkt,
        skalAktiveresEksterntTidspunkt = skalAktiveresTidspunkt,
        skalAktiveresBegrunnelse = "test",
        aktiveringstidspunkt = aktiveringstidspunkt,
        eksternAktiveringstidspunkt = aktiveringstidspunkt,
        opprettet = opprettet,
        sistEndret = aktiveringstidspunkt,
    )

    private fun sessionFactoryMedPropagerendeException(): SessionFactory {
        val transactionContext = mockk<TransactionContext>(relaxed = true)
        return mockk<SessionFactory>().also { sessionFactory ->
            every { sessionFactory.withTransactionContext(any<Function1<TransactionContext, Unit>>()) } answers {
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
