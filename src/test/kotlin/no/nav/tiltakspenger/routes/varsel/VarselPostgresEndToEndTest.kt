package no.nav.tiltakspenger.routes.varsel

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode.Companion.kanFyllesUtFraOgMed
import no.nav.tiltakspenger.meldekort.domene.varsler.Varsel
import no.nav.tiltakspenger.meldekort.repository.OptimistiskLåsFeil
import no.nav.tiltakspenger.meldekort.routes.meldekort.bruker.korrigering.MeldekortKorrigertDagDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.routes.jobber.KjørJobberForTester
import no.nav.tiltakspenger.routes.korrigering.korrigermeldekort.korrigerMeldekortRequest
import no.nav.tiltakspenger.routes.saksbehandling.mottaSakRequest
import no.nav.tiltakspenger.routes.sendinnmeldekort.sendInnNesteMeldekort
import no.nav.tiltakspenger.routes.withTestApplicationContextAndPostgres
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class VarselPostgresEndToEndTest {

    @Test
    fun `mottaSakRequest oppretter og aktiverer varsel når meldekortet kan sendes inn`() {
        val periode = 24.februar(2025) til 9.mars(2025)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = 1.mars(2025).atHour(10))),
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val aktivtVarsel = varsler.single()
            aktivtVarsel.shouldBeInstanceOf<Varsel.Aktiv>()
            aktivtVarsel.skalAktiveresTidspunkt.truncatedTo(ChronoUnit.MINUTES) shouldBe 10.mars(2025).atHour(10)

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 1
            varslerHendelser.sendteVarsler.single().varselId shouldBe aktivtVarsel.varselId
            varslerHendelser.inaktiverteVarsler shouldHaveSize 0
        }
    }

    @Test
    fun `mottaSakRequest oppretter ikke tilbakedatert varsel når meldekortet kunne sendes inn tidligere samme virkedag`() {
        val periode = 24.februar(2025) til 9.mars(2025)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atTime(10, 30)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = 1.mars(2025).atHour(10))),
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val aktivtVarsel = varsler.single().shouldBeInstanceOf<Varsel.Aktiv>()
            aktivtVarsel.skalAktiveresTidspunkt.truncatedTo(ChronoUnit.MINUTES) shouldBe 10.mars(2025).atTime(10, 30)
            aktivtVarsel.aktiveringstidspunkt.truncatedTo(ChronoUnit.MINUTES) shouldBe 10.mars(2025).atTime(10, 30)

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 1
            varslerHendelser.sendteVarsler.single().varselId shouldBe aktivtVarsel.varselId
            varslerHendelser.inaktiverteVarsler shouldHaveSize 0
        }
    }

    @Test
    fun `mottaSakRequest planlegger varsel til neste virkedag kl 09 når meldekortet kunne sendes inn tidligere men nå er etter stengetid`() {
        val periode = 24.februar(2025) til 9.mars(2025)
        val klokke = TikkendeKlokke(fixedClockAt(7.mars(2025).atTime(18, 0)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = 1.mars(2025).atHour(10))),
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val planlagtVarsel = varsler.single().shouldBeInstanceOf<Varsel.SkalAktiveres>()
            planlagtVarsel.skalAktiveresTidspunkt shouldBe 10.mars(2025).atHour(9)

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 0
            varslerHendelser.inaktiverteVarsler shouldHaveSize 0
        }
    }

    @Test
    fun `mottaSakRequest med ingen meldeperioder oppretter ingen varsler`() {
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = emptyList(),
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 0

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 0
            varslerHendelser.inaktiverteVarsler shouldHaveSize 0
        }
    }

    @Test
    fun `mottaSakRequest oppretter et planlagt varsel når meldekortet ikke kan sendes inn enda`() {
        val periode = 10 til 23.mars(2025)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = 1.mars(2025).atHour(10))),
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val planlagtVarsel = varsler.single()
            planlagtVarsel.shouldBeInstanceOf<Varsel.SkalAktiveres>()
            planlagtVarsel.skalAktiveresTidspunkt shouldBe periode.kanFyllesUtFraOgMed()

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 0
            varslerHendelser.inaktiverteVarsler shouldHaveSize 0
        }
    }

    @Test
    fun `mottaSakRequest med fremtidig meldeperiode avbryter planlagt varsel når ny versjon opphører perioden`() {
        val periode = 10 til 23.mars(2025)
        val opprettet = 1.mars(2025).atHour(10)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val førsteMeldeperiode = meldeperiodeDto(
                periode = periode,
                opprettet = opprettet,
            )
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(førsteMeldeperiode),
            )

            val planlagtVarsel = tac.varselRepo.hentForSakId(sak.id).single().shouldBeInstanceOf<Varsel.SkalAktiveres>().also {
                it.skalAktiveresTidspunkt shouldBe 21.mars(2025).atHour(15)
            }

            mottaSakRequest(
                tac = tac,
                fnr = sak.fnr,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                meldeperioder = listOf(
                    meldeperiodeDto(
                        periode = periode,
                        versjon = 2,
                        opprettet = opprettet.plusDays(1),
                        girRett = periode.tilDager().associateWith { false },
                        antallDagerForPeriode = 0,
                    ),
                ),
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val avbruttVarsel = varsler.single().shouldBeInstanceOf<Varsel.Avbrutt>()
            avbruttVarsel.varselId shouldBe planlagtVarsel.varselId

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 0
            varslerHendelser.inaktiverteVarsler shouldHaveSize 0
        }
    }

    @Test
    fun `mottaSakRequest med to fremtidige meldeperioder flytter planlagt varsel når første meldeperiode opphører`() {
        val førstePeriode = 10 til 23.mars(2025)
        val andrePeriode = 24.mars(2025) til 6.april(2025)
        val opprettet = 1.mars(2025).atHour(10)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val førsteMeldeperiode = meldeperiodeDto(
                periode = førstePeriode,
                opprettet = opprettet,
            )
            val andreMeldeperiode = meldeperiodeDto(
                periode = andrePeriode,
                opprettet = opprettet.plusMinutes(1),
            )

            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(førsteMeldeperiode, andreMeldeperiode),
            )

            val opprinneligVarsel = tac.varselRepo.hentForSakId(sak.id).single().shouldBeInstanceOf<Varsel.SkalAktiveres>()
            opprinneligVarsel.skalAktiveresTidspunkt shouldBe førstePeriode.kanFyllesUtFraOgMed()
            opprinneligVarsel.skalAktiveresBegrunnelse.shouldContain(førsteMeldeperiode.kjedeId)

            mottaSakRequest(
                tac = tac,
                fnr = sak.fnr,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                meldeperioder = listOf(
                    meldeperiodeDto(
                        periode = førstePeriode,
                        versjon = 2,
                        opprettet = opprettet.plusDays(1),
                        girRett = førstePeriode.tilDager().associateWith { false },
                        antallDagerForPeriode = 0,
                    ),
                    andreMeldeperiode,
                ),
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val oppdatertVarsel = varsler.single().shouldBeInstanceOf<Varsel.SkalAktiveres>()
            oppdatertVarsel.varselId shouldBe opprinneligVarsel.varselId
            oppdatertVarsel.skalAktiveresTidspunkt shouldBe andrePeriode.kanFyllesUtFraOgMed()
            oppdatertVarsel.skalAktiveresBegrunnelse.shouldContain(andreMeldeperiode.kjedeId)
            oppdatertVarsel.skalAktiveresBegrunnelse.shouldContain(andrePeriode.kanFyllesUtFraOgMed().toString())

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 0
            varslerHendelser.inaktiverteVarsler shouldHaveSize 0
        }
    }

    @Test
    fun `mottaSakRequest flytter nytt varsel til neste dag kl 09 når samme sak allerede har sendt varsel samme dag`() {
        val periode = 24.februar(2025) til 9.mars(2025)
        val opprettet = 1.mars(2025).atHour(10)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = opprettet)),
            )

            val sendtVarsel = tac.varselRepo.hentForSakId(sak.id).single().shouldBeInstanceOf<Varsel.Aktiv>()
            sendtVarsel.skalAktiveresTidspunkt.truncatedTo(ChronoUnit.MINUTES) shouldBe 10.mars(2025).atHour(10)

            mottaSakRequest(
                tac = tac,
                fnr = sak.fnr,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                meldeperioder = listOf(
                    meldeperiodeDto(
                        periode = periode,
                        versjon = 2,
                        opprettet = opprettet.plusDays(1),
                        girRett = periode.tilDager().associateWith { false },
                        antallDagerForPeriode = 0,
                    ),
                ),
            )

            val inaktivertVarsel = tac.varselRepo.hentForSakId(sak.id).single().shouldBeInstanceOf<Varsel.Inaktivert>()
            inaktivertVarsel.varselId shouldBe sendtVarsel.varselId

            mottaSakRequest(
                tac = tac,
                fnr = sak.fnr,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                meldeperioder = listOf(
                    meldeperiodeDto(
                        periode = periode,
                        versjon = 3,
                        opprettet = opprettet.plusDays(2),
                    ),
                ),
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 2
            varsler.filterIsInstance<Varsel.Inaktivert>().single().varselId shouldBe sendtVarsel.varselId
            val planlagtVarsel = varsler.filterIsInstance<Varsel.SkalAktiveres>().single()
            planlagtVarsel.skalAktiveresTidspunkt shouldBe 11.mars(2025).atHour(9)

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 1
            varslerHendelser.sendteVarsler.single().varselId shouldBe sendtVarsel.varselId
            varslerHendelser.inaktiverteVarsler shouldHaveSize 1
            varslerHendelser.inaktiverteVarsler.single() shouldBe sendtVarsel.varselId
        }
    }

    @Test
    fun `sendInnNesteMeldekort inaktiverer aktivt varsel etter innsending`() {
        val periode = 24.februar(2025) til 9.mars(2025)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = 1.mars(2025).atHour(10))),
            )

            sendInnNesteMeldekort(tac = tac)

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val inaktivertVarsel = varsler.single()
            inaktivertVarsel.shouldBeInstanceOf<Varsel.Inaktivert>()

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 1
            varslerHendelser.inaktiverteVarsler shouldHaveSize 1
            varslerHendelser.sendteVarsler.single().varselId shouldBe inaktivertVarsel.varselId
            varslerHendelser.inaktiverteVarsler.single() shouldBe inaktivertVarsel.varselId
        }
    }

    @Test
    fun `korrigerMeldekortRequest kjører varseljobber og inaktiverer aktivt varsel`() {
        val periode = 24.februar(2025) til 9.mars(2025)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = 1.mars(2025).atHour(10))),
            )

            val (_, innsendtMeldekort) = sendInnNesteMeldekort(tac = tac, runJobs = false)!!

            tac.varselRepo.hentForSakId(sak.id).single().shouldBeInstanceOf<Varsel.Aktiv>()

            val korrigerteDager = innsendtMeldekort.dager.mapIndexed { index, dag ->
                MeldekortKorrigertDagDTO(
                    dato = dag.dag,
                    status = if (index == 0) {
                        MeldekortDagStatus.FRAVÆR_SYK
                    } else {
                        dag.status
                    },
                )
            }

            korrigerMeldekortRequest(
                tac = tac,
                meldekortId = innsendtMeldekort.id.toString(),
                requestDto = korrigerteDager,
                locale = "nb",
            )

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val inaktivertVarsel = varsler.single()
            inaktivertVarsel.shouldBeInstanceOf<Varsel.Inaktivert>()

            val varslerHendelser = tac.varselClient.snapshotVarselhendelser()
            varslerHendelser.sendteVarsler shouldHaveSize 1
            varslerHendelser.inaktiverteVarsler shouldHaveSize 1
            varslerHendelser.inaktiverteVarsler.single() shouldBe inaktivertVarsel.varselId
        }
    }

    @Test
    fun `kjørVurderVarsler lar varsel som allerede skal inaktiveres stå uendret etter korrigering av meldekort`() {
        // Uke 9+10
        val periode = 24.februar(2025) til 9.mars(2025)
        // Mandagen etter meldeperioden. Tidspunktet skal være ubetydelig.
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = 1.mars(2025).atHour(10))),
            )
            // Vi ønsker å emulere at inaktiver-jobben har en delay.
            val (_, innsendtMeldekort) = sendInnNesteMeldekort(tac = tac, runJobs = false)!!
            // Derimot ønsker vi å få en ny vurdering, for å se at vi ikke inaktiverer på nytt.
            KjørJobberForTester.kjørVurderVarsler(tac)

            val varselSomSkalInaktiveres = tac.varselRepo.hentForSakId(sak.id).single().shouldBeInstanceOf<Varsel.SkalInaktiveres>()

            val varslerHendelserFørNyVurdering = tac.varselClient.snapshotVarselhendelser().also {
                it.sendteVarsler shouldHaveSize 1
                it.inaktiverteVarsler shouldHaveSize 0
            }

            val korrigerteDager = innsendtMeldekort.dager.mapIndexed { index, dag ->
                MeldekortKorrigertDagDTO(
                    dato = dag.dag,
                    status = if (index == 0) {
                        MeldekortDagStatus.FRAVÆR_SYK
                    } else {
                        dag.status
                    },
                )
            }

            korrigerMeldekortRequest(
                tac = tac,
                meldekortId = innsendtMeldekort.id.toString(),
                requestDto = korrigerteDager,
                locale = "nb",
                runJobs = false,
            )

            KjørJobberForTester.kjørVurderVarsler(tac)

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 1
            val fortsattSkalInaktiveres = varsler.single().shouldBeInstanceOf<Varsel.SkalInaktiveres>()
            fortsattSkalInaktiveres shouldBe varselSomSkalInaktiveres

            val varslerHendelserEtterNyVurdering = tac.varselClient.snapshotVarselhendelser()
            varslerHendelserEtterNyVurdering shouldBe varslerHendelserFørNyVurdering
        }
    }

    /**
     * Scenario fra bruker: sak med 2 innvilgede meldeperioder. Første varsel aktiveres,
     * bruker sender inn meldekort for første periode, varselet skal inaktiveres. Deretter
     * skal et NYTT varsel opprettes for den andre meldeperioden – selv uten at nye
     * hendelser kommer inn fra saksbehandling. Dette krever at InaktiverVarslerService
     * re-flagger saken for vurdering etter inaktivering.
     */
    @Test
    fun `2 meldeperioder - etter innsending av første meldekort opprettes nytt varsel for andre meldeperiode`() {
        val førstePeriode = 24.februar(2025) til 9.mars(2025)
        val andrePeriode = 10 til 23.mars(2025)
        val opprettet = 1.mars(2025).atHour(10)
        // Mandag etter første periode - varsel for første periode er allerede aktivt (kanFyllesUt var fredag).
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(
                    meldeperiodeDto(periode = førstePeriode, opprettet = opprettet),
                    meldeperiodeDto(periode = andrePeriode, opprettet = opprettet.plusMinutes(1)),
                ),
            )

            // Første varsel skal være aktivert.
            val førsteVarsel = tac.varselRepo.hentForSakId(sak.id).single().shouldBeInstanceOf<Varsel.Aktiv>()

            // Bruker sender inn meldekort for første periode. kjørVarsler kjøres inni sendInnNesteMeldekort:
            //   - VurderVarsel: Aktiv -> SkalInaktiveres (andre meldeperiode mangler fortsatt innsending,
            //     men nyMeldeperiode = andrePeriode.kanFyllesUtFraOgMed > førsteVarsel.skalAktiveresTidspunkt)
            //   - InaktiverVarsler: SkalInaktiveres -> Inaktivert, og re-flagger saken for vurdering.
            // Så i neste kjøring av varseljobbene skal et nytt varsel opprettes.
            sendInnNesteMeldekort(tac = tac)

            // Etter innsending: første varsel inaktivert, sak re-flagget for vurdering.
            tac.varselRepo.hentForSakId(sak.id).single { it.varselId == førsteVarsel.varselId }
                .shouldBeInstanceOf<Varsel.Inaktivert>()
            tac.sakVarselRepo.hentSakerSomSkalVurdereVarsel().map { it.sak.id } shouldBe listOf(sak.id)

            // Kjør varseljobbene en gang til for å simulere neste scheduled run.
            KjørJobberForTester.kjørVarsler(tac)

            val varsler = tac.varselRepo.hentForSakId(sak.id)
            varsler shouldHaveSize 2
            varsler.filterIsInstance<Varsel.Inaktivert>().single().varselId shouldBe førsteVarsel.varselId
            val nyttVarsel = varsler.filterIsInstance<Varsel.SkalAktiveres>().single()
            nyttVarsel.skalAktiveresTidspunkt shouldBe andrePeriode.kanFyllesUtFraOgMed()
            nyttVarsel.varselId shouldNotBe førsteVarsel.varselId
        }
    }

    /**
     * Regresjonstest for samtidighetsproblemet på `sak.sist_flagget_tidspunkt`:
     *
     *  1. Et meldekort mottas → `flaggForVarselvurdering` setter flagg=true og oppdaterer
     *     `sist_flagget_tidspunkt` via clock_timestamp().
     *  2. Varseljobben starter og leser sakene som skal vurderes (flagg=true, med tidspunkt).
     *  3. Under jobbens transaksjon kommer det inn et NYTT meldekort som på nytt kaller
     *     `flaggForVarselvurdering` – dette oppdaterer `sist_flagget_tidspunkt` til en ny
     *     verdi.
     *  4. Jobben kaller `markerVarselVurdert` med tidspunktet den leste i steg 2. Optimistisk
     *     lås oppdager at tidspunktet er endret og kaster [OptimistiskLåsFeil]. Transaksjonen
     *     ruller tilbake og saken forblir flagget.
     *  5. Saken plukkes opp i neste kjøring og vurderes på nytt – ingen hendelse går tapt.
     */
    @Test
    fun `optimistisk lås - flaggForVarselvurdering under pågående varseljobb bevares`() {
        val periode = 24.februar(2025) til 9.mars(2025)
        val klokke = TikkendeKlokke(fixedClockAt(10.mars(2025).atHour(10)))

        withTestApplicationContextAndPostgres(clock = klokke, runIsolated = true) { tac ->
            // 1. Opprett sak og kjør varseljobbene slik at flagget settes til false (sak er vurdert).
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = 1.mars(2025).atHour(10))),
            )
            tac.sakVarselRepo.hentSakerSomSkalVurdereVarsel().map { it.sak.id } shouldBe emptyList()

            // 2. Simuler at et meldekort mottas: flagget settes til true.
            tac.sakVarselRepo.flaggForVarselvurdering(sak.id)
            val sakerFørJobb = tac.sakVarselRepo.hentSakerSomSkalVurdereVarsel()
            sakerFørJobb.map { it.sak.id } shouldBe listOf(sak.id)
            val sistFlaggetTidspunktVedLesing = sakerFørJobb.single().sistFlaggetTidspunkt

            // 3. Simuler racet: jobben åpner sin transaksjon, leser saken, og MENS den holder
            //    på, kommer det et nytt meldekort (ny transaksjon som flagger for vurdering).
            //    Jobben forsøker deretter markerVarselVurdert med tidspunktet fra steg 2.
            val feil = shouldThrow<Throwable> {
                tac.sessionFactory.withTransactionContext { txJobb ->
                    val sakerJobbenLeste = tac.sakVarselRepo.hentSakerSomSkalVurdereVarsel(sessionContext = txJobb)
                    sakerJobbenLeste.map { it.sak.id } shouldBe listOf(sak.id)

                    // Samtidig transaksjon som simulerer et nytt meldekort som mottas.
                    // Committer før jobben fullfører sin markerVarselVurdert.
                    tac.sessionFactory.withTransactionContext { txMeldekort ->
                        tac.sakVarselRepo.flaggForVarselvurdering(sak.id, sessionContext = txMeldekort)
                    }

                    // Jobben fullfører sin vurdering, men optimistisk lås skal avvise
                    // oppdateringen siden sist_flagget_tidspunkt er endret i mellomtiden.
                    tac.sakVarselRepo.markerVarselVurdert(
                        sakId = sak.id,
                        vurdertTidspunkt = nå(klokke),
                        sistFlaggetTidspunktVedLesing = sistFlaggetTidspunktVedLesing,
                        sessionContext = txJobb,
                    )
                }
            }

            // 4. Optimistisk lås slo til – exception kastet og jobbens transaksjon rulles tilbake.
            feil.shouldBeInstanceOf<OptimistiskLåsFeil>()

            // 5. Saken forblir flagget slik at den plukkes opp på nytt i neste kjøring.
            tac.sakVarselRepo.hentSakerSomSkalVurdereVarsel().map { it.sak.id } shouldBe listOf(sak.id)
        }
    }
}

private fun LocalDate.atHour(time: Int): LocalDateTime = this.atTime(time, 0)
