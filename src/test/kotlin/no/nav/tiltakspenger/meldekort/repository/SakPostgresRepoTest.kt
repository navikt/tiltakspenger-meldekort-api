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
    private val offset = nå(fixedClock).toLocalDate().minusMonths(1)
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
        fun `saker med meldeperiode innenfor offset returneres`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sakMedMeldeperiodeInnenfor6Måneder = lagSakMedMeldeperiode(tilSisteSøndagEtter = offset.plusDays(14)) // Garanterer at perioden innenfor offset med siste søndag 14 dager før.
                val sakMedMeldeperiodeSeksMånederBakover = lagSakMedMeldeperiode(tilSisteSøndagEtter = offset)
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
                val sakMeldeperiodeSeksmånederBakover = lagSakMedMeldeperiode(tilSisteSøndagEtter = offset)
                val sakMeldeperiodeNylig = lagSakMedMeldeperiode(tilSisteSøndagEtter = nå(fixedClock).toLocalDate())
                lagreSak(helper, sakMeldeperiodeNylig, sakMeldeperiodeSeksmånederBakover)

                val saker = repo.hentSakerHvorMicrofrontendSkalAktiveres(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(sakMeldeperiodeNylig.id, saker[0].id, "Sak")
            }
        }
    }

    @Nested
    inner class HentSakerHvorMicrofrontendSkalInaktiveres {
        @Test
        fun `saker med meldeperiode til og med offset antall måneder siden blir returnert`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val gammelSak = lagSakMedMeldeperiode(tilSisteSøndagEtter = offset)
                val nySak = ObjectMother.sak(fnr = Fnr.random())
                lagreSak(helper, gammelSak, nySak)

                val saker = repo.hentSakerHvorMicrofrontendSkalInaktiveres(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(gammelSak.id, saker[0].id, "Sak")
            }
        }

        @Test
        fun `saker med meldeperiode mindre enn offset antall måneder siden blir ikke returnert`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val gammelSak = lagSakMedMeldeperiode(tilSisteSøndagEtter = offset.plusMonths(1))
                val nySak = ObjectMother.sak(fnr = Fnr.random())
                lagreSak(helper, gammelSak, nySak)

                val saker = repo.hentSakerHvorMicrofrontendSkalInaktiveres(clock = fixedClock)

                assertEquals(0, saker.size, "Antall saker")
            }
        }

        @Test
        fun `saker med meldeperiode mer enn offset antall måneder siden blir returnert`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo

                val gammelSak = lagSakMedMeldeperiode(tilSisteSøndagEtter = offset.minusMonths(1))
                val nySak = ObjectMother.sak(fnr = Fnr.random())
                lagreSak(helper, gammelSak, nySak)

                val saker = repo.hentSakerHvorMicrofrontendSkalInaktiveres(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(gammelSak.id, saker[0].id, "Sak")
            }
        }
    }
}
