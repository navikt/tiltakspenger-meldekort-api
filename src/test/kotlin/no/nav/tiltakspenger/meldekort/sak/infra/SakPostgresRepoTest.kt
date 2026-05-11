package no.nav.tiltakspenger.meldekort.sak.infra

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.microfrontend.MicrofrontendStatus
import no.nav.tiltakspenger.meldekort.sak.ArenaMeldekortStatus
import no.nav.tiltakspenger.meldekort.sak.Sak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SakPostgresRepoTest {
    private val offsetMonths = 1L
    private val offset = nå(fixedClock).toLocalDate().minusMonths(offsetMonths)
    private val innenforOffset = offset.plusMonths(offsetMonths)
    private val utenforOffset = offset.minusMonths(1)
    private fun lagreSak(helper: TestDataHelper, vararg saker: Sak) {
        saker.forEach {
            helper.sakPostgresRepo.lagre(it)
            it.meldeperioder.forEach { mp -> helper.meldeperiodeRepo.lagre(mp) }
        }
    }

    /**
     * Genererer en sak med en meldeperiode hvor vi setter tilOgMed
     */
    private fun lagSakMedMeldeperiode(
        fraSisteMandagFør: LocalDate? = null,
        tilSisteSøndagEtter: LocalDate? = null,
        opprettet: LocalDate,
    ): Sak {
        var periode: Periode? = null
        fraSisteMandagFør?.let { periode = ObjectMother.periode(fraSisteMandagFør = fraSisteMandagFør) }
        tilSisteSøndagEtter?.let { periode = ObjectMother.periode(tilSisteSøndagEtter = tilSisteSøndagEtter) }

        val meldeperiode = ObjectMother.meldeperiode(opprettet = opprettet.atStartOfDay(), periode = periode!!)
        return ObjectMother.sak(
            id = meldeperiode.sakId,
            fnr = meldeperiode.fnr,
            saksnummer = meldeperiode.saksnummer,
            meldeperioder = listOf(meldeperiode),
        )
    }

    @Nested
    inner class GrunnleggendeSpørringer {

        @Test
        fun `lagrer og henter sak med meldeperioder`() {
            withMigratedDb { helper ->
                val meldeperiode1 = ObjectMother.meldeperiode(opprettet = nå(fixedClock))
                val meldeperiode2 = ObjectMother.meldeperiode(
                    periode = meldeperiode1.periode.plus14Dager(),
                    fnr = meldeperiode1.fnr,
                    sakId = meldeperiode1.sakId,
                    saksnummer = meldeperiode1.saksnummer,
                    opprettet = nå(fixedClock).plusDays(14),
                )
                val sak = ObjectMother.sak(
                    id = meldeperiode1.sakId,
                    fnr = meldeperiode1.fnr,
                    saksnummer = meldeperiode1.saksnummer,
                    meldeperioder = listOf(meldeperiode1, meldeperiode2),
                    arenaMeldekortStatus = ArenaMeldekortStatus.HAR_MELDEKORT,
                    harSoknadUnderBehandling = true,
                    kanSendeInnHelgForMeldekort = true,
                )

                lagreSak(helper, sak)

                helper.sakPostgresRepo.hent(sak.id) shouldBe sak
            }
        }

        @Test
        fun `hent returnerer null for ukjent sakId`() {
            withMigratedDb { helper ->
                helper.sakPostgresRepo.hent(SakId.random()) shouldBe null
            }
        }

        @Test
        fun `hentForBruker returnerer sak uten meldeperioder`() {
            withMigratedDb { helper ->
                val meldeperiode = ObjectMother.meldeperiode(opprettet = nå(fixedClock))
                val sak = ObjectMother.sak(
                    id = meldeperiode.sakId,
                    fnr = meldeperiode.fnr,
                    saksnummer = meldeperiode.saksnummer,
                    meldeperioder = listOf(meldeperiode),
                )

                lagreSak(helper, sak)

                helper.sakPostgresRepo.hentForBruker(sak.fnr) shouldBe sak.copy(meldeperioder = emptyList())
            }
        }

        @Test
        fun `hentForBruker returnerer null for ukjent fnr`() {
            withMigratedDb { helper ->
                helper.sakPostgresRepo.hentForBruker(Fnr.random()) shouldBe null
            }
        }

        @Test
        fun `harSak returnerer true bare når bruker har sak`() {
            withMigratedDb { helper ->
                val sak = ObjectMother.sak(fnr = Fnr.random())

                helper.sakPostgresRepo.lagre(sak)

                helper.sakPostgresRepo.harSak(sak.fnr) shouldBe true
                helper.sakPostgresRepo.harSak(Fnr.random()) shouldBe false
            }
        }
    }

    @Nested
    inner class Oppdateringer {

        @Test
        fun `oppdater endrer fnr og saksbehandlingsflagg men beholder arena status`() {
            withMigratedDb { helper ->
                val opprinneligSak = ObjectMother.sak(
                    fnr = Fnr.random(),
                    arenaMeldekortStatus = ArenaMeldekortStatus.HAR_MELDEKORT,
                    harSoknadUnderBehandling = false,
                    kanSendeInnHelgForMeldekort = false,
                )
                helper.sakPostgresRepo.lagre(opprinneligSak)

                val oppdatertSak = opprinneligSak.copy(
                    fnr = Fnr.random(),
                    arenaMeldekortStatus = ArenaMeldekortStatus.HAR_IKKE_MELDEKORT,
                    harSoknadUnderBehandling = true,
                    kanSendeInnHelgForMeldekort = true,
                )
                helper.sakPostgresRepo.oppdater(oppdatertSak)

                helper.sakPostgresRepo.hent(opprinneligSak.id) shouldBe oppdatertSak.copy(
                    arenaMeldekortStatus = opprinneligSak.arenaMeldekortStatus,
                )
            }
        }

        @Test
        fun `oppdaterFnr endrer fnr for alle saker med gammelt fnr`() {
            withMigratedDb { helper ->
                val gammeltFnr = Fnr.random()
                val nyttFnr = Fnr.random()
                val sak = ObjectMother.sak(fnr = gammeltFnr)
                helper.sakPostgresRepo.lagre(sak)

                helper.sakPostgresRepo.oppdaterFnr(gammeltFnr, nyttFnr)

                helper.sakPostgresRepo.hent(sak.id) shouldBe sak.copy(fnr = nyttFnr)
                helper.sakPostgresRepo.hentForBruker(gammeltFnr) shouldBe null
                helper.sakPostgresRepo.hentForBruker(nyttFnr) shouldBe sak.copy(fnr = nyttFnr)
            }
        }

        @Test
        fun `oppdaterArenaStatus endrer bare arena status`() {
            withMigratedDb { helper ->
                val sak = ObjectMother.sak(
                    fnr = Fnr.random(),
                    arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
                    harSoknadUnderBehandling = true,
                    kanSendeInnHelgForMeldekort = true,
                )
                helper.sakPostgresRepo.lagre(sak)

                helper.sakPostgresRepo.oppdaterArenaStatus(sak.id, ArenaMeldekortStatus.HAR_IKKE_MELDEKORT)

                helper.sakPostgresRepo.hent(sak.id) shouldBe sak.copy(
                    arenaMeldekortStatus = ArenaMeldekortStatus.HAR_IKKE_MELDEKORT,
                )
            }
        }

        @Test
        fun `oppdaterStatusForMicrofrontend setter aktiv og inaktiv status`() {
            withMigratedDb { helper ->
                val sak = ObjectMother.sak(fnr = Fnr.random())
                helper.sakPostgresRepo.lagre(sak)

                helper.sakPostgresRepo.oppdaterStatusForMicrofrontend(sak.id, aktiv = true)
                hentMicrofrontendStatus(helper, sak.id) shouldBe MicrofrontendStatus.AKTIV

                helper.sakPostgresRepo.oppdaterStatusForMicrofrontend(sak.id, aktiv = false)
                hentMicrofrontendStatus(helper, sak.id) shouldBe MicrofrontendStatus.INAKTIV
            }
        }
    }

    @Nested
    inner class HentSakerUtenArenaStatus {

        @Test
        fun `returnerer bare saker med ukjent arena status`() {
            withMigratedDb { helper ->
                val sakMedUkjentStatus = ObjectMother.sak(fnr = Fnr.random(), arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT)
                val sakMedMeldekort = ObjectMother.sak(fnr = Fnr.random(), arenaMeldekortStatus = ArenaMeldekortStatus.HAR_MELDEKORT)
                val sakUtenMeldekort = ObjectMother.sak(fnr = Fnr.random(), arenaMeldekortStatus = ArenaMeldekortStatus.HAR_IKKE_MELDEKORT)
                lagreSak(helper, sakMedUkjentStatus, sakMedMeldekort, sakUtenMeldekort)

                helper.sakPostgresRepo.hentSakerUtenArenaStatus() shouldBe listOf(sakMedUkjentStatus.copy(meldeperioder = emptyList()))
            }
        }
    }

    @Nested
    inner class HentSakerHvorMicrofrontendSkalAktiveres {

        @Test
        fun `returneres - meldeperiode innenfor offset`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sak1 = lagSakMedMeldeperiode(tilSisteSøndagEtter = innenforOffset, opprettet = utenforOffset)
                val sak2 = lagSakMedMeldeperiode(tilSisteSøndagEtter = utenforOffset, opprettet = utenforOffset)
                lagreSak(helper, sak1, sak2)

                val saker = repo.hentSakerHvorMicrofrontendSkalAktiveres()

                withClue("Antall saker") { saker.size shouldBe 1 }
                withClue("Sak") { saker.single().id shouldBe sak1.id }
            }
        }

        @Test
        fun `returneres - opprettet innenfor offset`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sak1 = lagSakMedMeldeperiode(tilSisteSøndagEtter = utenforOffset, opprettet = innenforOffset)
                val sak2 = lagSakMedMeldeperiode(tilSisteSøndagEtter = utenforOffset, opprettet = utenforOffset)
                lagreSak(helper, sak1, sak2)

                val saker = repo.hentSakerHvorMicrofrontendSkalAktiveres()

                withClue("Antall saker") { saker.size shouldBe 1 }
                withClue("Sak") { saker.single().id shouldBe sak1.id }
            }
        }

        @Test
        fun `returneres - meldeperiode og opprettet innenfor offset`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sak1 = lagSakMedMeldeperiode(tilSisteSøndagEtter = innenforOffset, opprettet = innenforOffset)
                val sak2 = lagSakMedMeldeperiode(tilSisteSøndagEtter = utenforOffset, opprettet = utenforOffset)
                lagreSak(helper, sak1, sak2)

                val saker = repo.hentSakerHvorMicrofrontendSkalAktiveres()

                withClue("Antall saker") { saker.size shouldBe 1 }
                withClue("Sak") { saker.single().id shouldBe sak1.id }
            }
        }

        @Test
        fun `returnerer ikke saker som allerede er aktivert eller mangler dager som gir rett`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val aktivSak = lagSakMedMeldeperiode(tilSisteSøndagEtter = innenforOffset, opprettet = innenforOffset)
                val meldeperiodeUtenRett = ObjectMother.meldeperiode(
                    periode = ObjectMother.periode(tilSisteSøndagEtter = innenforOffset),
                    opprettet = innenforOffset.atStartOfDay(),
                    girRett = ObjectMother.periode(tilSisteSøndagEtter = innenforOffset).tilDager().associateWith { false },
                )
                val sakUtenRett = ObjectMother.sak(
                    id = meldeperiodeUtenRett.sakId,
                    fnr = meldeperiodeUtenRett.fnr,
                    saksnummer = meldeperiodeUtenRett.saksnummer,
                    meldeperioder = listOf(meldeperiodeUtenRett),
                )
                lagreSak(helper, aktivSak, sakUtenRett)
                repo.oppdaterStatusForMicrofrontend(aktivSak.id, aktiv = true)

                repo.hentSakerHvorMicrofrontendSkalAktiveres() shouldBe emptyList()
            }
        }
    }

    @Nested
    inner class HentSakerHvorMicrofrontendSkalInaktiveres {

        @Test
        fun `returneres ikke - opprettet innenfor offset`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sak1 = lagSakMedMeldeperiode(tilSisteSøndagEtter = utenforOffset, opprettet = utenforOffset)
                val sak2 = lagSakMedMeldeperiode(tilSisteSøndagEtter = utenforOffset, opprettet = innenforOffset)
                lagreSak(helper, sak1, sak2)

                val saker = repo.hentSakerHvorMicrofrontendSkalInaktiveres()

                withClue("Antall saker") { saker.size shouldBe 1 }
                withClue("Forventer at sak med opprettet utenfor offset returneres") { saker.single().id shouldBe sak1.id }
            }
        }

        @Test
        fun `returneres ikke - meldeperiode innenfor offset`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sak1 = lagSakMedMeldeperiode(tilSisteSøndagEtter = utenforOffset, opprettet = utenforOffset)
                val sak2 = lagSakMedMeldeperiode(tilSisteSøndagEtter = innenforOffset, opprettet = utenforOffset)
                lagreSak(helper, sak1, sak2)

                val saker = repo.hentSakerHvorMicrofrontendSkalInaktiveres()

                withClue("Antall saker") { saker.size shouldBe 1 }
                withClue("Forventer at sak med meldeperiode utenfor offset returneres") { saker.single().id shouldBe sak1.id }
            }
        }

        @Test
        fun `returneres - meldeperiode og opprettet utenfor offset`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sak1 = lagSakMedMeldeperiode(tilSisteSøndagEtter = utenforOffset, opprettet = utenforOffset)
                val sak2 = lagSakMedMeldeperiode(tilSisteSøndagEtter = innenforOffset, opprettet = innenforOffset)
                lagreSak(helper, sak1, sak2)

                val saker = repo.hentSakerHvorMicrofrontendSkalInaktiveres()

                withClue("Antall saker") { saker.size shouldBe 1 }
                withClue("Forventer at sak med begge felter utenfor offset returneres") { saker.single().id shouldBe sak1.id }
            }
        }

        @Test
        fun `returnerer ikke saker som allerede er inaktivert`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sak = lagSakMedMeldeperiode(tilSisteSøndagEtter = utenforOffset, opprettet = utenforOffset)
                lagreSak(helper, sak)
                repo.oppdaterStatusForMicrofrontend(sak.id, aktiv = false)

                repo.hentSakerHvorMicrofrontendSkalInaktiveres() shouldBe emptyList()
            }
        }
    }

    private fun hentMicrofrontendStatus(helper: TestDataHelper, sakId: SakId): MicrofrontendStatus {
        return helper.sessionFactory.withSession { session ->
            session.run(
                queryOf(
                    "select microfrontend_status from sak where id = :id",
                    mapOf("id" to sakId.toString()),
                ).map { row -> MicrofrontendStatus.valueOf(row.string("microfrontend_status")) }.asSingle,
            )!!
        }
    }
}
