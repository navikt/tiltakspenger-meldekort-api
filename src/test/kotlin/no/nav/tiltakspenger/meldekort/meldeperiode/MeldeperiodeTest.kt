package no.nav.tiltakspenger.meldekort.meldeperiode

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.november
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode.Companion.kanFyllesUtFraOgMed
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MeldeperiodeTest {
    private val id = MeldeperiodeId.random()
    private val sakId = SakId.random()
    private val fnr = Fnr.random()
    private val saksnummer = "saksnummer"

    @Test
    fun `2 meldeperioder er like selv om opprettet og kanFyllesUtFraOgMed er ulik`() {
        val m1 = ObjectMother.meldeperiode(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = nå(fixedClock),
            kanFyllesUtFraOgMed = nå(fixedClock),
        )
        val m2 = ObjectMother.meldeperiode(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = nå(fixedClock),
            kanFyllesUtFraOgMed = nå(fixedClock).plusSeconds(1),
        )

        m1.erLik(m2) shouldBe true
    }

    @Test
    fun `2 meldeperioder er ikke like`() {
        val m1 = ObjectMother.meldeperiode(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = nå(fixedClock),
            kanFyllesUtFraOgMed = nå(fixedClock),
        )
        val m2 = ObjectMother.meldeperiode(
            id = id,
            sakId = SakId.random(),
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = nå(fixedClock),
            kanFyllesUtFraOgMed = nå(fixedClock),
        )

        m1.erLik(m2) shouldBe false
    }

    @Nested
    inner class FinnerNærmesteFredagInnenforPeriodenOgLeggerPåRiktigTidspunkt {
        @Test
        fun `tilOgMed = torsdag - velger fredag som er '1 uke tilbake'`() {
            val periode = Periode(fraOgMed = 10.november(2025), tilOgMed = 20.november(2025))

            val actual = periode.kanFyllesUtFraOgMed()

            actual shouldBe 14.november(2025).atTime(15, 0, 0)
        }

        @Test
        fun `tilOgMed = lørdag - velger fredagen som er før lørdagen`() {
            val periode = Periode(fraOgMed = 15.november(2025), tilOgMed = 22.november(2025))

            val actual = periode.kanFyllesUtFraOgMed()

            actual shouldBe 21.november(2025).atTime(15, 0, 0)
        }

        @Test
        fun `perioden inneholder ikke en fredag`() {
            val periode = Periode(fraOgMed = 19.november(2025), tilOgMed = 20.november(2025))

            assertThrows<IllegalArgumentException> {
                periode.kanFyllesUtFraOgMed()
            }
        }
    }
}
