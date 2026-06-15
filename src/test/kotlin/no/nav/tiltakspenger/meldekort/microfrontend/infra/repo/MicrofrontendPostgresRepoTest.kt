package no.nav.tiltakspenger.meldekort.microfrontend.infra.repo

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tiltakspenger.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.meldekort.mottak.infra.routes.mottaSakRequest
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import org.junit.jupiter.api.Test

/**
 * Fokuserte "happy case"-tester for [no.nav.tiltakspenger.meldekort.microfrontend.infra.repo.MicrofrontendPostgresRepo] – én per spørring/funksjon.
 *
 * Full dekning av kriteriene (gir rett, allerede aktivert/inaktivert, siste versjon, ingen oppgave igjen, limit) ligger i [no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendEndToEndTest].
 *
 * All seeding går via [mottaSakRequest] (ytterste nivå), ikke direkte mot basen.
 */
class MicrofrontendPostgresRepoTest {

    @Test
    fun `oppdaterStatusForMicrofrontend setter aktiv og inaktiv status`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = mottaSakRequest(tac = tac, runJobs = false)

            tac.microfrontendRepo.oppdaterStatusForMicrofrontend(sak.id, aktiv = true).getOrFail()
            hentStatus(tac, sak.id) shouldBe MicrofrontendStatusDb.AKTIV

            tac.microfrontendRepo.oppdaterStatusForMicrofrontend(sak.id, aktiv = false).getOrFail()
            hentStatus(tac, sak.id) shouldBe MicrofrontendStatusDb.INAKTIV
        }
    }

    @Test
    fun `hentSakerHvorMicrofrontendSkalAktiveres returnerer sak med meldeperiode som gir rett`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(
                    meldeperiodeDto(periode = ObjectMother.periode(tilSisteSøndagEtter = 13.april(2025)), opprettet = 15.april(2025).atTime(10, 0)),
                ),
                runJobs = false,
            )

            tac.microfrontendRepo.hentSakerHvorMicrofrontendSkalAktiveres(limit = Int.MAX_VALUE).getOrFail().map { it.sakId } shouldContain sak.id
        }
    }

    @Test
    fun `hentSakerHvorMicrofrontendSkalInaktiveres returnerer sak uten meldeperiode som gir rett`() {
        withTestApplicationContextAndPostgres { tac ->
            val periode = ObjectMother.periode(tilSisteSøndagEtter = 13.april(2025))
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(
                    meldeperiodeDto(
                        periode = periode,
                        opprettet = 15.april(2025).atTime(10, 0),
                        girRett = periode.tilDager().associateWith { false },
                    ),
                ),
                runJobs = false,
            )

            tac.microfrontendRepo.hentSakerHvorMicrofrontendSkalInaktiveres(limit = Int.MAX_VALUE).getOrFail().map { it.sakId } shouldContain sak.id
        }
    }

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
