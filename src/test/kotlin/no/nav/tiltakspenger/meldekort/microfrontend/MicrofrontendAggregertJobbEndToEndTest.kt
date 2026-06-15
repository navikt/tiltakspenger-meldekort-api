package no.nav.tiltakspenger.meldekort.microfrontend

import arrow.core.Either
import arrow.core.left
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.fakes.clients.TmsMikrofrontendClientFake
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottaSakRequest
import no.nav.tiltakspenger.meldekort.sak.Sak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * EndToEnd-tester for de aggregerte microfrontend-jobbene ([AktiverMicrofrontendJob.aktiverMicrofrontendForBrukere] /
 * [InaktiverMicrofrontendJob.inaktiverMicrofrontendForBrukere]) og feilhåndteringen deres.
 *
 * Disse kjører isolert ([withTestApplicationContextAndPostgres] med `runIsolated = true`) fordi resultatet er
 * avhengig av kryss-sak-spørringene [MicrofrontendRepo.hentSakerHvorMicrofrontendSkalAktiveres] /
 * [MicrofrontendRepo.hentSakerHvorMicrofrontendSkalInaktiveres] – de ser på alle saker i basen samtidig.
 *
 * Utfallet sjekkes mot [MicrofrontendJobbResultat] som jobbene returnerer (vellykkede/feilede saker), ikke
 * mot recording-faken. Per-sak-oppførsel (én bruker) testes uten isolasjon i [MicrofrontendEndToEndTest].
 * All seeding går via [mottaSakRequest] (ytterste nivå, slik som i prod).
 */
class MicrofrontendAggregertJobbEndToEndTest {

    private val periode: Periode = ObjectMother.periode(tilSisteSøndagEtter = 13.april(2025))
    private val opprettet: LocalDateTime = 15.april(2025).atTime(10, 0)

    @Nested
    inner class Aktivering {

        @Test
        fun `aggregert jobb aktiverer alle saker som skal aktiveres`() {
            withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
                val sak1 = mottaSak(tac, periode = periode, opprettet = opprettet)
                val sak2 = mottaSak(tac, periode = periode, opprettet = opprettet)

                val resultat = tac.aktiverMicrofrontendJob.aktiverMicrofrontendForBrukere()

                resultat.vellykkede shouldContainExactlyInAnyOrder listOf(sak1.id, sak2.id)
                resultat.feilede shouldBe emptyList()
                tac.microfrontendRepo.hentSakerHvorMicrofrontendSkalAktiveres().getOrFail() shouldBe emptyList()
            }
        }

        @Test
        fun `aggregert jobb skiller vellykkede fra feilede saker`() {
            withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
                val vellykket = mottaSak(tac, periode = periode, opprettet = opprettet)
                val feilet = mottaSak(tac, periode = periode, opprettet = opprettet)
                tac.tmsMikrofrontendClient.kastFeilFor(feilet.id)

                val resultat = tac.aktiverMicrofrontendJob.aktiverMicrofrontendForBrukere()

                resultat.vellykkede shouldBe listOf(vellykket.id)
                resultat.feilede shouldBe listOf(feilet.id)
            }
        }

        @Test
        fun `respekterer limit`() {
            withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
                mottaSak(tac, periode = periode, opprettet = opprettet)
                mottaSak(tac, periode = periode, opprettet = opprettet)

                tac.microfrontendRepo.hentSakerHvorMicrofrontendSkalAktiveres(limit = 1).getOrFail().size shouldBe 1
            }
        }
    }

    @Nested
    inner class Inaktivering {

        @Test
        fun `aggregert jobb inaktiverer alle saker som skal inaktiveres`() {
            withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
                val sak1 = mottaSakUtenRett(tac, periode = periode, opprettet = opprettet)
                val sak2 = mottaSakUtenRett(tac, periode = periode, opprettet = opprettet)

                val resultat = tac.inaktiverMicrofrontendJob.inaktiverMicrofrontendForBrukere()

                resultat.vellykkede shouldContainExactlyInAnyOrder listOf(sak1.id, sak2.id)
                resultat.feilede shouldBe emptyList()
                tac.microfrontendRepo.hentSakerHvorMicrofrontendSkalInaktiveres().getOrFail() shouldBe emptyList()
            }
        }

        @Test
        fun `aggregert jobb skiller vellykkede fra feilede saker`() {
            withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
                val vellykket = mottaSakUtenRett(tac, periode = periode, opprettet = opprettet)
                val feilet = mottaSakUtenRett(tac, periode = periode, opprettet = opprettet)
                tac.tmsMikrofrontendClient.kastFeilFor(feilet.id)

                val resultat = tac.inaktiverMicrofrontendJob.inaktiverMicrofrontendForBrukere()

                resultat.vellykkede shouldBe listOf(vellykket.id)
                resultat.feilede shouldBe listOf(feilet.id)
            }
        }

        @Test
        fun `respekterer limit`() {
            withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
                mottaSakUtenRett(tac, periode = periode, opprettet = opprettet)
                mottaSakUtenRett(tac, periode = periode, opprettet = opprettet)

                tac.microfrontendRepo.hentSakerHvorMicrofrontendSkalInaktiveres(limit = 1).getOrFail().size shouldBe 1
            }
        }
    }

    @Nested
    inner class HentingFeiler {

        @Test
        fun `aktivering gir tomt resultat dersom henting av saker feiler`() {
            val job = AktiverMicrofrontendJob(FeilendeMicrofrontendRepo, TmsMikrofrontendClientFake())

            job.aktiverMicrofrontendForBrukere() shouldBe MicrofrontendJobbResultat.tom
        }

        @Test
        fun `inaktivering gir tomt resultat dersom henting av saker feiler`() {
            val job = InaktiverMicrofrontendJob(FeilendeMicrofrontendRepo, TmsMikrofrontendClientFake())

            job.inaktiverMicrofrontendForBrukere() shouldBe MicrofrontendJobbResultat.tom
        }
    }

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

    /** Repo som returnerer [MicrofrontendFeil.DatabaseFeil] ved henting, for å teste at jobbene håndterer feil uten å kaste. */
    private object FeilendeMicrofrontendRepo : MicrofrontendRepo {
        override fun oppdaterStatusForMicrofrontend(sakId: SakId, aktiv: Boolean, sessionContext: SessionContext?) =
            error("skal ikke kalles")

        override fun hentSakerHvorMicrofrontendSkalAktiveres(limit: Int, sessionContext: SessionContext?): Either<MicrofrontendFeil, List<MicrofrontendSak>> =
            MicrofrontendFeil.DatabaseFeil(RuntimeException("simulert databasefeil")).left()

        override fun hentSakerHvorMicrofrontendSkalInaktiveres(limit: Int, sessionContext: SessionContext?): Either<MicrofrontendFeil, List<MicrofrontendSak>> =
            MicrofrontendFeil.DatabaseFeil(RuntimeException("simulert databasefeil")).left()

        override fun hentMeldekortInfo(fnr: Fnr, sessionContext: SessionContext?) =
            error("skal ikke kalles")
    }
}
