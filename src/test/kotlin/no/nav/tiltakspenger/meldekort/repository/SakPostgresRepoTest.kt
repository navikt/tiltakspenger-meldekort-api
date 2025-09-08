package no.nav.tiltakspenger.meldekort.repository

import no.nav.tiltakspenger.db.TestDataHelper
import no.nav.tiltakspenger.db.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.meldekort.domene.Sak
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SakPostgresRepoTest {
    private fun lagreSak(helper: TestDataHelper, vararg saker: Sak) {
        saker.forEach {
            it.meldeperioder.forEach { mp -> helper.meldeperiodeRepo.lagre(mp) }
            helper.sakPostgresRepo.lagre(it)
        }
    }

    @Nested
    inner class HentSakerHvorSistePeriodeMedRettighetErLengeSiden {
        @Test
        fun `saker med meldeperiode til og med 6 måneder siden blir returnert`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sakId = SakId.random()
                val meldeperiode = ObjectMother.meldeperiode(
                    sakId = sakId,
                    periode = ObjectMother.periode(tilSisteSøndagEtter = nå(fixedClock).toLocalDate().minusMonths(6)),
                )

                val gammelSak = ObjectMother.sak(id = sakId, fnr = Fnr.random(), meldeperioder = listOf(meldeperiode))
                val nySak = ObjectMother.sak(fnr = Fnr.random())
                lagreSak(helper, gammelSak, nySak)

                val saker = repo.hentSakerHvorSistePeriodeMedRettighetErLengeSiden(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(sakId, saker[0].id, "Sak")
            }
        }

        @Test
        fun `saker med meldeperiode mindre enn 6 måneder siden blir ikke returnert`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sakId = SakId.random()
                val meldeperiode = ObjectMother.meldeperiode(
                    sakId = sakId,
                    periode = ObjectMother.periode(tilSisteSøndagEtter = nå(fixedClock).toLocalDate().minusMonths(5)),
                )

                val gammelSak = ObjectMother.sak(id = sakId, fnr = Fnr.random(), meldeperioder = listOf(meldeperiode))
                val nySak = ObjectMother.sak(fnr = Fnr.random())
                lagreSak(helper, gammelSak, nySak)

                val saker = repo.hentSakerHvorSistePeriodeMedRettighetErLengeSiden(clock = fixedClock)

                assertEquals(0, saker.size, "Antall saker")
            }
        }

        @Test
        fun `saker med meldeperiode mer enn 6 måneder siden blir ikke returnert`() {
            withMigratedDb { helper ->
                val repo = helper.sakPostgresRepo
                val sakId = SakId.random()
                val meldeperiode = ObjectMother.meldeperiode(
                    sakId = sakId,
                    periode = ObjectMother.periode(tilSisteSøndagEtter = nå(fixedClock).toLocalDate().minusMonths(7)),
                )

                val gammelSak = ObjectMother.sak(id = sakId, fnr = Fnr.random(), meldeperioder = listOf(meldeperiode))
                val nySak = ObjectMother.sak(fnr = Fnr.random())
                lagreSak(helper, gammelSak, nySak)

                val saker = repo.hentSakerHvorSistePeriodeMedRettighetErLengeSiden(clock = fixedClock)

                assertEquals(1, saker.size, "Antall saker")
                assertEquals(sakId, saker[0].id, "Sak")
            }
        }
    }
}
