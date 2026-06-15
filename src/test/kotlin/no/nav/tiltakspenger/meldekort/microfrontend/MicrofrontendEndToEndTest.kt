package no.nav.tiltakspenger.meldekort.microfrontend

import arrow.core.right
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import kotliquery.queryOf
import no.nav.tiltakspenger.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.fakes.clients.TmsMikrofrontendClientFake
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.sendInnNesteMeldekort
import no.nav.tiltakspenger.meldekort.microfrontend.infra.repo.MicrofrontendStatusDb
import no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottaSakRequest
import no.nav.tiltakspenger.meldekort.sak.Sak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * EndToEnd-tester for microfrontend på sak-nivå. All seeding går via [mottaSakRequest] (ytterste nivå,
 * slik som i prod) – vi bygger ikke opp data direkte i basen.
 *
 * Disse testene rører kun én konkret sak (per-sak-jobbene, eller kriterie-sjekk av at nettopp denne saken
 * er med/ikke med i kryss-sak-spørringen) og kjører derfor uten isolasjon. Kriterietestene sjekker bevisst
 * mot saken sin egen id med høy `limit`, slik at resultatet er deterministisk uavhengig av andre saker i basen.
 *
 * Den aggregerte jobb-oppførselen (aktiver/inaktiver alle, limit, ukjent feil) testes isolert i
 * [MicrofrontendAggregertJobbEndToEndTest].
 *
 * Kriteriet speiler bevisst varsler-pakken sin «mangler innsending»-logikk (se [no.nav.tiltakspenger.meldekort.varsler.infra.VarselMeldekortPostgresRepo]): en sak er relevant så lenge den har minst én meldeperiodekjede der *siste versjon* gir rett til å fylle ut og verken bruker eller saksbehandler har sendt inn/behandlet.
 * Det finnes bevisst *ingen* tidsvindu – kortet vises «evig» til oppgaven er løst, akkurat som varslene.
 */
class MicrofrontendEndToEndTest {

    private val periode: Periode = ObjectMother.periode(tilSisteSøndagEtter = 13.april(2025))
    private val opprettet: LocalDateTime = 15.april(2025).atTime(10, 0)

    // En gammel meldeperiode (både periode og opprettet langt tilbake i tid).
    // Brukes til å vise at kortet vises «evig» – det finnes ikke lenger noe tidsvindu som skrur kortet av basert på alder.
    private val gammelPeriode: Periode = ObjectMother.periode(tilSisteSøndagEtter = 16.februar(2020))
    private val gammelOpprettet: LocalDateTime = 3.februar(2020).atTime(10, 0)

    // Default-klokka er 1. mai 2025. «Tidligere» kjeder kan fylles ut nå; «fremtidig» kjede kan først fylles ut senere.
    private val tidligereKjedeA: Periode = ObjectMother.periode(tilSisteSøndagEtter = 13.april(2025))
    private val tidligereKjedeB: Periode = ObjectMother.periode(tilSisteSøndagEtter = 30.mars(2025))
    private val fremtidigKjede: Periode = ObjectMother.periode(tilSisteSøndagEtter = 22.juni(2025))

    @Nested
    inner class Aktivering {

        @Test
        fun `aktiverer microfrontend og oppdaterer status for en enkelt sak`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = periode, opprettet = opprettet)

                val resultat = tac.aktiverMicrofrontendJob.aktiverMicrofrontendForBruker(sak.fnr, sak.id)

                resultat shouldBe Unit.right()
                tac.tmsMikrofrontendClient.aktiverte() shouldBe listOf(
                    TmsMikrofrontendClientFake.MicrofrontendBruker(sak.fnr, sak.id),
                )
                hentStatus(tac, sak.id) shouldBe MicrofrontendStatusDb.AKTIV
            }
        }

        @Test
        fun `svelger feil og lar status være urørt når aktivering mot klienten kaster`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = periode, opprettet = opprettet)
                tac.tmsMikrofrontendClient.kastFeilFor(sak.id)

                val resultat = tac.aktiverMicrofrontendJob.aktiverMicrofrontendForBruker(sak.fnr, sak.id)

                resultat.isLeft() shouldBe true
                tac.tmsMikrofrontendClient.aktiverte() shouldBe emptyList()
                hentStatus(tac, sak.id) shouldBe MicrofrontendStatusDb.UBEHANDLET
            }
        }

        @Test
        fun `aktiveres - meldeperiode som gir rett uten innsending`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = periode, opprettet = opprettet)

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe true
            }
        }

        @Test
        fun `aktiveres - kortet vises evig selv for en gammel meldeperiode`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = gammelPeriode, opprettet = gammelOpprettet)

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe true
            }
        }

        @Test
        fun `returneres ikke - allerede aktivert`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = periode, opprettet = opprettet)
                tac.microfrontendRepo.oppdaterStatusForMicrofrontend(sak.id, aktiv = true).getOrFail()

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe false
            }
        }

        @Test
        fun `returneres ikke - mangler dager som gir rett`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(
                    tac,
                    periode = periode,
                    opprettet = opprettet,
                    girRett = periode.tilDager().associateWith { false },
                )

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe false
            }
        }
    }

    @Nested
    inner class Inaktivering {

        @Test
        fun `inaktiverer microfrontend og oppdaterer status for en enkelt sak`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSakUtenRett(tac, periode = periode, opprettet = opprettet)

                val resultat = tac.inaktiverMicrofrontendJob.inaktiverMicrofrontendForBruker(sak.fnr, sak.id)

                resultat shouldBe Unit.right()
                tac.tmsMikrofrontendClient.inaktiverte() shouldBe listOf(
                    TmsMikrofrontendClientFake.MicrofrontendBruker(sak.fnr, sak.id),
                )
                hentStatus(tac, sak.id) shouldBe MicrofrontendStatusDb.INAKTIV
            }
        }

        @Test
        fun `svelger feil og lar status være urørt når inaktivering mot klienten kaster`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSakUtenRett(tac, periode = periode, opprettet = opprettet)
                tac.tmsMikrofrontendClient.kastFeilFor(sak.id)

                val resultat = tac.inaktiverMicrofrontendJob.inaktiverMicrofrontendForBruker(sak.fnr, sak.id)

                resultat.isLeft() shouldBe true
                tac.tmsMikrofrontendClient.inaktiverte() shouldBe emptyList()
                hentStatus(tac, sak.id) shouldBe MicrofrontendStatusDb.UBEHANDLET
            }
        }

        @Test
        fun `inaktiveres - ingen meldeperiode som gir rett`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSakUtenRett(tac, periode = periode, opprettet = opprettet)

                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe true
            }
        }

        @Test
        fun `returneres ikke - har fortsatt en åpen oppgave som gir rett`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = periode, opprettet = opprettet)

                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe false
            }
        }

        @Test
        fun `returneres ikke - har åpen oppgave selv for en gammel meldeperiode`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = gammelPeriode, opprettet = gammelOpprettet)

                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe false
            }
        }

        @Test
        fun `returneres ikke - allerede inaktivert`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSakUtenRett(tac, periode = periode, opprettet = opprettet)
                tac.microfrontendRepo.oppdaterStatusForMicrofrontend(sak.id, aktiv = false).getOrFail()

                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe false
            }
        }
    }

    /**
     * Kun siste versjon av en meldeperiode-kjede skal styre om microfrontend aktiveres/inaktiveres.
     * En eldre versjon som gir rett skal ikke kunne "overstyre" en nyere versjon som ikke gir rett, og motsatt.
     *
     * Dette er måten vi håndterer annullering / stans / opphør på: en meldeperiodekjede gjelder for eksakt 14 dager, og når et nytt rammevedtak påvirker kjeden lager vi en ny versjon – den forrige er da "annullert" og gjelder ikke lenger.
     * Ved alltid å ta utgangspunkt i siste versjon skrus kortet automatisk av når retten faller bort (ingen falske positive).
     * Eventuelle allerede innsendte meldekort på en utdatert versjon består (notoritet).
     */
    @Nested
    inner class SisteVersjon {

        @Test
        fun `aktiveres ikke når siste versjon ikke gir rett selv om eldre versjon gjorde det`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSakMedToVersjoner(
                    tac,
                    periode = periode,
                    opprettet = opprettet,
                    girRettVersjon1 = periode.tilDager().associateWith { true },
                    girRettVersjon2 = periode.tilDager().associateWith { false },
                )

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe false
            }
        }

        @Test
        fun `inaktiveres når siste versjon ikke gir rett selv om eldre versjon gjorde det`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSakMedToVersjoner(
                    tac,
                    periode = periode,
                    opprettet = opprettet,
                    girRettVersjon1 = periode.tilDager().associateWith { true },
                    girRettVersjon2 = periode.tilDager().associateWith { false },
                )

                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe true
            }
        }

        @Test
        fun `aktiveres når siste versjon gir rett selv om eldre versjon ikke gjorde det`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSakMedToVersjoner(
                    tac,
                    periode = periode,
                    opprettet = opprettet,
                    girRettVersjon1 = periode.tilDager().associateWith { false },
                    girRettVersjon2 = periode.tilDager().associateWith { true },
                )

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe true
            }
        }
    }

    /**
     * Vi skal ikke vise meldekort-boksen når det ikke lenger er en "oppgave" igjen for meldeperioden, dvs. når enten brukeren selv eller saksbehandler allerede har sendt inn/behandlet minst ett meldekort for perioden.
     */
    @Nested
    inner class IngenOppgaveIgjen {

        @Test
        fun `vises ikke når bruker allerede har sendt inn meldekortet`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSakRequest(
                    tac = tac,
                    meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = opprettet)),
                    runJobs = false,
                )

                sendInnNesteMeldekort(tac = tac, fnr = sak.fnr.verdi, runJobs = false)

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe false
                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe true
            }
        }

        @Test
        fun `vises ikke når saksbehandler har behandlet meldeperioden`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSakMedSaksbehandlerbehandling(tac, periode = periode, opprettet = opprettet)

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe false
                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe true
            }
        }

        @Test
        fun `vises ikke når bruker sendte inn versjon 1 og en ny versjon av samme kjede senere kommer`() {
            withTestApplicationContextAndPostgres { tac ->
                val fnr = tac.nesteFnr()
                val saksnummer = tac.nesteSaksnummer()
                val sakId = SakId.random()

                mottaSakRequest(
                    tac = tac,
                    fnr = fnr,
                    sakId = sakId,
                    saksnummer = saksnummer,
                    meldeperioder = listOf(meldeperiodeDto(periode = periode, versjon = 1, opprettet = opprettet)),
                    runJobs = false,
                )
                sendInnNesteMeldekort(tac = tac, fnr = fnr.verdi, runJobs = false)

                val sak = mottaSakRequest(
                    tac = tac,
                    fnr = fnr,
                    sakId = sakId,
                    saksnummer = saksnummer,
                    meldeperioder = listOf(meldeperiodeDto(periode = periode, versjon = 2, opprettet = opprettet.plusDays(1))),
                    runJobs = false,
                )

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe false
                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe true
            }
        }
    }

    /**
     * Tidshorisont og flere kjeder: kortet vises så lenge minst én kjede mangler innsending – uavhengig av om kjeden
     * kan fylles ut nå («tidligere») eller først senere («fremtidig»).
     * En fremtidig kjede holder altså kortet åpent selv om den ikke kan sendes inn ennå.
     */
    @Nested
    inner class FlereKjederOgTidshorisont {

        @Test
        fun `flere tidligere kjeder uten innsending - vises`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSakRequest(
                    tac = tac,
                    meldeperioder = listOf(
                        meldeperiodeDto(periode = tidligereKjedeB, opprettet = opprettet),
                        meldeperiodeDto(periode = tidligereKjedeA, opprettet = opprettet),
                    ),
                    runJobs = false,
                )

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe true
                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe false
            }
        }

        @Test
        fun `flere tidligere kjeder alle sendt inn av bruker - vises ikke`() {
            withTestApplicationContextAndPostgres { tac ->
                val fnr = tac.nesteFnr()
                val sak = mottaSakRequest(
                    tac = tac,
                    fnr = fnr,
                    meldeperioder = listOf(
                        meldeperiodeDto(periode = tidligereKjedeB, opprettet = opprettet),
                        meldeperiodeDto(periode = tidligereKjedeA, opprettet = opprettet),
                    ),
                    runJobs = false,
                )

                sendInnNesteMeldekort(tac = tac, fnr = fnr.verdi, runJobs = false)
                sendInnNesteMeldekort(tac = tac, fnr = fnr.verdi, runJobs = false)

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe false
                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe true
            }
        }

        @Test
        fun `flere tidligere kjeder alle behandlet av saksbehandler - vises ikke`() {
            withTestApplicationContextAndPostgres { tac ->
                val mpA = meldeperiodeDto(periode = tidligereKjedeA, opprettet = opprettet)
                val mpB = meldeperiodeDto(periode = tidligereKjedeB, opprettet = opprettet)
                val sak = mottaSakRequest(
                    tac = tac,
                    requestDto = ObjectMother.sakDTO(
                        fnr = tac.nesteFnr().verdi,
                        saksnummer = tac.nesteSaksnummer(),
                        meldeperioder = listOf(mpA, mpB),
                        meldekortvedtak = listOf(
                            ObjectMother.meldekortvedtakDTO(meldeperiode = mpA, opprettet = opprettet),
                            ObjectMother.meldekortvedtakDTO(meldeperiode = mpB, opprettet = opprettet),
                        ),
                    ),
                    runJobs = false,
                )

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe false
                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe true
            }
        }

        @Test
        fun `kun fremtidig kjede uten innsending - vises selv om den ikke kan fylles ut ennå`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSakRequest(
                    tac = tac,
                    meldeperioder = listOf(meldeperiodeDto(periode = fremtidigKjede, opprettet = opprettet)),
                    runJobs = false,
                )

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe true
                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe false
            }
        }

        @Test
        fun `blanding - tidligere sendt inn men fremtidig mangler innsending - vises`() {
            withTestApplicationContextAndPostgres { tac ->
                val fnr = tac.nesteFnr()
                val sak = mottaSakRequest(
                    tac = tac,
                    fnr = fnr,
                    meldeperioder = listOf(
                        meldeperiodeDto(periode = tidligereKjedeA, opprettet = opprettet),
                        meldeperiodeDto(periode = fremtidigKjede, opprettet = opprettet),
                    ),
                    runJobs = false,
                )

                // Bruker kan kun sende inn den tidligere kjeden; den fremtidige kan ikke fylles ut ennå.
                sendInnNesteMeldekort(tac = tac, fnr = fnr.verdi, runJobs = false)

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe true
                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe false
            }
        }
    }

    private suspend fun ApplicationTestBuilder.mottaSakMedSaksbehandlerbehandling(
        tac: TestApplicationContextMedPostgres,
        periode: Periode,
        opprettet: LocalDateTime,
    ): Sak {
        val meldeperiode = meldeperiodeDto(periode = periode, opprettet = opprettet)
        return mottaSakRequest(
            tac = tac,
            requestDto = ObjectMother.sakDTO(
                fnr = tac.nesteFnr().verdi,
                saksnummer = tac.nesteSaksnummer(),
                meldeperioder = listOf(meldeperiode),
                meldekortvedtak = listOf(ObjectMother.meldekortvedtakDTO(meldeperiode = meldeperiode, opprettet = opprettet)),
            ),
            runJobs = false,
        )
    }

    private suspend fun ApplicationTestBuilder.mottaSakMedToVersjoner(
        tac: TestApplicationContextMedPostgres,
        periode: Periode,
        opprettet: LocalDateTime,
        girRettVersjon1: Map<LocalDate, Boolean>,
        girRettVersjon2: Map<LocalDate, Boolean>,
    ): Sak = mottaSakRequest(
        tac = tac,
        meldeperioder = listOf(
            meldeperiodeDto(periode = periode, versjon = 1, opprettet = opprettet, girRett = girRettVersjon1),
            meldeperiodeDto(periode = periode, versjon = 2, opprettet = opprettet.plusDays(1), girRett = girRettVersjon2),
        ),
        runJobs = false,
    )

    private suspend fun ApplicationTestBuilder.mottaSak(
        tac: TestApplicationContextMedPostgres,
        periode: Periode,
        opprettet: LocalDateTime,
        girRett: Map<LocalDate, Boolean> = periode.tilDager().associateWith { true },
    ): Sak = mottaSakRequest(
        tac = tac,
        meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = opprettet, girRett = girRett)),
        runJobs = false,
    )

    /** En sak uten en åpen oppgave: siste versjon gir ikke rett på noen dag (opphør/ingen rett). */
    private suspend fun ApplicationTestBuilder.mottaSakUtenRett(
        tac: TestApplicationContextMedPostgres,
        periode: Periode,
        opprettet: LocalDateTime,
    ): Sak = mottaSak(tac, periode = periode, opprettet = opprettet, girRett = periode.tilDager().associateWith { false })

    /**
     * Sjekker om nettopp [sak] er blant sakene kryss-sak-spørringen vil aktivere. Bruker høy limit slik at
     * resultatet ikke påvirkes av hvor mange andre saker som tilfeldigvis ligger i den delte test-basen.
     */
    private fun MicrofrontendRepo.skalAktiveres(sak: Sak): Boolean =
        hentSakerHvorMicrofrontendSkalAktiveres(limit = Int.MAX_VALUE).getOrFail().map { it.sakId }.contains(sak.id)

    private fun MicrofrontendRepo.skalInaktiveres(sak: Sak): Boolean =
        hentSakerHvorMicrofrontendSkalInaktiveres(limit = Int.MAX_VALUE).getOrFail().map { it.sakId }.contains(sak.id)

    private fun hentStatus(tac: TestApplicationContextMedPostgres, sakId: SakId): MicrofrontendStatusDb =
        tac.sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    "select microfrontend_status from sak where id = :id",
                    mapOf("id" to sakId.toString()),
                ).map { row -> MicrofrontendStatusDb.valueOf(row.string("microfrontend_status")) }.asSingle,
            )!!
        }
}
