package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.Sak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class SakPostgresRepoTest {
    private val seksMånederBakover = nå(fixedClock).toLocalDate().minusMonths(6)
    private fun lagreSak(helper: TestDataHelper, vararg saker: Sak) {
        saker.forEach {
            it.meldeperioder.forEach { mp -> helper.meldeperiodeRepo.lagre(mp) }
            helper.sakPostgresRepo.lagre(it)
        }
    }

    /**
     * Genererer en sak med en meldeperiode hvor vi setter tilOgMed
     */
    private fun lagSakMedMeldeperiode(fraSisteMandagFør: LocalDate? = null, tilSisteSøndagEtter: LocalDate? = null): Sak {
        var periode: Periode? = null
        fraSisteMandagFør?.let { periode = ObjectMother.periode(fraSisteMandagFør = fraSisteMandagFør) }
        tilSisteSøndagEtter?.let { periode = ObjectMother.periode(tilSisteSøndagEtter = tilSisteSøndagEtter) }

        val meldeperiode = ObjectMother.meldeperiode(periode = periode!!)
        return ObjectMother.sak(id = meldeperiode.sakId, fnr = Fnr.random(), meldeperioder = listOf(meldeperiode))
    }

    @Nested
    inner class HentSakerHvorMicrofrontendSkalAktiveres {
        @Test
        fun `saker med meldeperiode innenfor siste 6 måneder returneres`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sakMedMeldeperiodeInnenfor6Måneder = lagSakMedMeldeperiode(tilSisteSøndagEtter = seksMånederBakover.plusDays(14)) // Garanterer at perioden innenfor 6 måneder med siste søndag 14 dager før.
                val sakMedMeldeperiodeSeksMånederBakover = lagSakMedMeldeperiode(tilSisteSøndagEtter = seksMånederBakover)
                lagreSak(helper, sakMedMeldeperiodeInnenfor6Måneder, sakMedMeldeperiodeSeksMånederBakover)

                val saker = repo.hentSakerHvorMicrofrontendSkalAktiveres(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(sakMedMeldeperiodeInnenfor6Måneder.id, saker[0].id, "Sak")
            }
        }

        @Test
        fun `saker med meldeperiode nær dagens dato`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sakMeldeperiodeSeksmånederBakover = lagSakMedMeldeperiode(tilSisteSøndagEtter = seksMånederBakover)
                val sakMeldeperiodeNylig = lagSakMedMeldeperiode(tilSisteSøndagEtter = nå(fixedClock).toLocalDate())
                lagreSak(helper, sakMeldeperiodeNylig, sakMeldeperiodeSeksmånederBakover)

                val saker = repo.hentSakerHvorMicrofrontendSkalAktiveres(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(sakMeldeperiodeNylig.id, saker[0].id, "Sak")
            }
        }
    }

    @Nested
    inner class HentSakferHvorMicrofrontendSkalInaktiveres {
        @Test
        fun `saker med meldeperiode til og med 6 måneder siden blir returnert`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val gammelSak = lagSakMedMeldeperiode(tilSisteSøndagEtter = seksMånederBakover)
                val nySak = ObjectMother.sak(fnr = Fnr.random())
                lagreSak(helper, gammelSak, nySak)

                val saker = repo.hentSakerHvorMicrofrontendSkalInaktiveres(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(gammelSak.id, saker[0].id, "Sak")
            }
        }

        @Test
        fun `saker med meldeperiode mindre enn 6 måneder siden blir ikke returnert`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val gammelSak = lagSakMedMeldeperiode(tilSisteSøndagEtter = seksMånederBakover.plusMonths(1))
                val nySak = ObjectMother.sak(fnr = Fnr.random())
                lagreSak(helper, gammelSak, nySak)

                val saker = repo.hentSakerHvorMicrofrontendSkalInaktiveres(clock = fixedClock)

                assertEquals(0, saker.size, "Antall saker")
            }
        }

        @Test
        fun `saker med meldeperiode mer enn 6 måneder siden blir returnert`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo

                val gammelSak = lagSakMedMeldeperiode(tilSisteSøndagEtter = seksMånederBakover.minusMonths(1))
                val nySak = ObjectMother.sak(fnr = Fnr.random())
                lagreSak(helper, gammelSak, nySak)

                val saker = repo.hentSakerHvorMicrofrontendSkalInaktiveres(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(gammelSak.id, saker[0].id, "Sak")
            }
        }
    }
}
