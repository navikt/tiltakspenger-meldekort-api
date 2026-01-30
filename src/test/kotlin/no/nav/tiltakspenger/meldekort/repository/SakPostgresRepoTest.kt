package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.domene.Sak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class SakPostgresRepoTest {
    private val offsetMonths = 1L
    private val offset = nå(fixedClock).toLocalDate().minusMonths(offsetMonths)
    private val innenforOffset = offset.plusMonths(offsetMonths)
    private val utenforOffset = offset.minusMonths(1)
    private fun lagreSak(helper: TestDataHelper, vararg saker: Sak) {
        saker.forEach {
            it.meldeperioder.forEach { mp -> helper.meldeperiodeRepo.lagre(mp) }
            helper.sakPostgresRepo.lagre(it)
        }
    }

    /**
     * Genererer en sak med en meldeperiode hvor vi setter tilOgMed
     */
    private fun lagSakMedMeldeperiode(fraSisteMandagFør: LocalDate? = null, tilSisteSøndagEtter: LocalDate? = null, opprettet: LocalDate?): Sak {
        var periode: Periode? = null
        fraSisteMandagFør?.let { periode = ObjectMother.periode(fraSisteMandagFør = fraSisteMandagFør) }
        tilSisteSøndagEtter?.let { periode = ObjectMother.periode(tilSisteSøndagEtter = tilSisteSøndagEtter) }

        val meldeperiode = ObjectMother.meldeperiode(opprettet = opprettet?.atStartOfDay(), periode = periode!!)
        return ObjectMother.sak(id = meldeperiode.sakId, fnr = Fnr.random(), meldeperioder = listOf(meldeperiode))
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

                val saker = repo.hentSakerHvorMicrofrontendSkalAktiveres(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(sak1.id, saker.single().id, "Sak")
            }
        }

        @Test
        fun `returneres - opprettet innenfor offset`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sak1 = lagSakMedMeldeperiode(tilSisteSøndagEtter = utenforOffset, opprettet = innenforOffset)
                val sak2 = lagSakMedMeldeperiode(tilSisteSøndagEtter = utenforOffset, opprettet = utenforOffset)
                lagreSak(helper, sak1, sak2)

                val saker = repo.hentSakerHvorMicrofrontendSkalAktiveres(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(sak1.id, saker.single().id, "Sak")
            }
        }

        @Test
        fun `returneres - meldeperiode og opprettet innenfor offset`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sak1 = lagSakMedMeldeperiode(tilSisteSøndagEtter = innenforOffset, opprettet = innenforOffset)
                val sak2 = lagSakMedMeldeperiode(tilSisteSøndagEtter = utenforOffset, opprettet = utenforOffset)
                lagreSak(helper, sak1, sak2)

                val saker = repo.hentSakerHvorMicrofrontendSkalAktiveres(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(sak1.id, saker.single().id, "Sak")
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

                val saker = repo.hentSakerHvorMicrofrontendSkalInaktiveres(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(sak1.id, saker.single().id, "Forventer at sak med opprettet utenfor offset returneres")
            }
        }

        @Test
        fun `returneres ikke - meldeperiode innenfor offset`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sak1 = lagSakMedMeldeperiode(tilSisteSøndagEtter = utenforOffset, opprettet = utenforOffset)
                val sak2 = lagSakMedMeldeperiode(tilSisteSøndagEtter = innenforOffset, opprettet = utenforOffset)
                lagreSak(helper, sak1, sak2)

                val saker = repo.hentSakerHvorMicrofrontendSkalInaktiveres(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(sak1.id, saker.single().id, "Forventer at sak med meldeperiode utenfor offset returneres")
            }
        }

        @Test
        fun `returneres - meldeperiode og opprettet utenfor offset`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sak1 = lagSakMedMeldeperiode(tilSisteSøndagEtter = utenforOffset, opprettet = utenforOffset)
                val sak2 = lagSakMedMeldeperiode(tilSisteSøndagEtter = innenforOffset, opprettet = innenforOffset)
                lagreSak(helper, sak1, sak2)

                val saker = repo.hentSakerHvorMicrofrontendSkalInaktiveres(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(sak1.id, saker.single().id, "Forventer at sak med begge felter utenfor offset returneres")
            }
        }
    }
}
