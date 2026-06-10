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
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContextAndPostgres
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
 */
class MicrofrontendEndToEndTest {

    // Offset i spørringene er nå(clock).minusMonths(1). Default-klokka er 1. mai 2025, så offset ~ 1. april 2025.
    private val innenforPeriode: Periode = ObjectMother.periode(tilSisteSøndagEtter = 13.april(2025))
    private val utenforPeriode: Periode = ObjectMother.periode(tilSisteSøndagEtter = 16.februar(2025))
    private val innenforOpprettet: LocalDateTime = 15.april(2025).atTime(10, 0)
    private val utenforOpprettet: LocalDateTime = 3.februar(2025).atTime(10, 0)

    @Nested
    inner class Aktivering {

        @Test
        fun `aktiverer microfrontend og oppdaterer status for en enkelt sak`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = innenforPeriode, opprettet = innenforOpprettet)

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
                val sak = mottaSak(tac, periode = innenforPeriode, opprettet = innenforOpprettet)
                tac.tmsMikrofrontendClient.kastFeilFor(sak.id)

                val resultat = tac.aktiverMicrofrontendJob.aktiverMicrofrontendForBruker(sak.fnr, sak.id)

                resultat.isLeft() shouldBe true
                tac.tmsMikrofrontendClient.aktiverte() shouldBe emptyList()
                hentStatus(tac, sak.id) shouldBe MicrofrontendStatusDb.UBEHANDLET
            }
        }

        @Test
        fun `returneres - meldeperiode innenfor offset`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = innenforPeriode, opprettet = utenforOpprettet)

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe true
            }
        }

        @Test
        fun `returneres - opprettet innenfor offset`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = utenforPeriode, opprettet = innenforOpprettet)

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe true
            }
        }

        @Test
        fun `returneres - meldeperiode og opprettet innenfor offset`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = innenforPeriode, opprettet = innenforOpprettet)

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe true
            }
        }

        @Test
        fun `returneres ikke - allerede aktivert`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = innenforPeriode, opprettet = innenforOpprettet)
                tac.microfrontendRepo.oppdaterStatusForMicrofrontend(sak.id, aktiv = true).getOrFail()

                tac.microfrontendRepo.skalAktiveres(sak) shouldBe false
            }
        }

        @Test
        fun `returneres ikke - mangler dager som gir rett`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(
                    tac,
                    periode = innenforPeriode,
                    opprettet = innenforOpprettet,
                    girRett = innenforPeriode.tilDager().associateWith { false },
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
                val sak = mottaSak(tac, periode = utenforPeriode, opprettet = utenforOpprettet)

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
                val sak = mottaSak(tac, periode = utenforPeriode, opprettet = utenforOpprettet)
                tac.tmsMikrofrontendClient.kastFeilFor(sak.id)

                val resultat = tac.inaktiverMicrofrontendJob.inaktiverMicrofrontendForBruker(sak.fnr, sak.id)

                resultat.isLeft() shouldBe true
                tac.tmsMikrofrontendClient.inaktiverte() shouldBe emptyList()
                hentStatus(tac, sak.id) shouldBe MicrofrontendStatusDb.UBEHANDLET
            }
        }

        @Test
        fun `returneres - meldeperiode og opprettet utenfor offset`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = utenforPeriode, opprettet = utenforOpprettet)

                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe true
            }
        }

        @Test
        fun `returneres ikke - opprettet innenfor offset`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = utenforPeriode, opprettet = innenforOpprettet)

                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe false
            }
        }

        @Test
        fun `returneres ikke - meldeperiode innenfor offset`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = innenforPeriode, opprettet = utenforOpprettet)

                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe false
            }
        }

        @Test
        fun `returneres ikke - allerede inaktivert`() {
            withTestApplicationContextAndPostgres { tac ->
                val sak = mottaSak(tac, periode = utenforPeriode, opprettet = utenforOpprettet)
                tac.microfrontendRepo.oppdaterStatusForMicrofrontend(sak.id, aktiv = false).getOrFail()

                tac.microfrontendRepo.skalInaktiveres(sak) shouldBe false
            }
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
