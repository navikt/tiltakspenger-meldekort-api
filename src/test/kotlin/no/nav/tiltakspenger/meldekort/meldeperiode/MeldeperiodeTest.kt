package no.nav.tiltakspenger.meldekort.meldeperiode

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.november
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.meldeperiode.Meldeperiode.Companion.kanFyllesUtFraOgMed
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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

            shouldThrow<IllegalArgumentException> {
                periode.kanFyllesUtFraOgMed()
            }
        }
    }

    @Nested
    inner class SisteMeldeperiodeFørJul {
        @Test
        fun `jul 2026 - meldeperioden 7-20 desember kan fylles ut fra onsdag 16 desember`() {
            Periode(7.desember(2026), 20.desember(2026)).kanFyllesUtFraOgMed() shouldBe 16.desember(2026).atTime(15, 0)
        }

        @Test
        fun `jul 2025 - meldeperioden 8-21 desember kan fylles ut fra onsdag 17 desember`() {
            Periode(8.desember(2025), 21.desember(2025)).kanFyllesUtFraOgMed() shouldBe 17.desember(2025).atTime(15, 0)
        }

        @Test
        fun `slutter 11 desember - kan fylles ut fra siste onsdag`() {
            Periode(28.november(2033), 11.desember(2033)).kanFyllesUtFraOgMed() shouldBe 7.desember(2033).atTime(15, 0)
        }

        @Test
        fun `slutter 22 desember - siste onsdag er etter 17 desember, så kan fylles ut fra tirsdag 17 desember`() {
            Periode(9.desember(2030), 22.desember(2030)).kanFyllesUtFraOgMed() shouldBe 17.desember(2030).atTime(15, 0)
        }

        @Test
        fun `slutter 23 desember - siste onsdag er etter 17 desember, så kan fylles ut fra mandag 17 desember`() {
            Periode(10.desember(2035), 23.desember(2035)).kanFyllesUtFraOgMed() shouldBe 17.desember(2035).atTime(15, 0)
        }

        @Test
        fun `slutter på julaften - 17 desember er søndag, så kan fylles ut fra fredag 15 desember`() {
            Periode(11.desember(2034), 24.desember(2034)).kanFyllesUtFraOgMed() shouldBe 15.desember(2034).atTime(15, 0)
        }

        @Test
        fun `slutter 10 desember - er ikke siste meldeperiode før jul og kan fylles ut fra fredag`() {
            Periode(27.november(2034), 10.desember(2034)).kanFyllesUtFraOgMed() shouldBe 8.desember(2034).atTime(15, 0)
        }

        @Test
        fun `slutter 25 desember - er ikke før jul og kan fylles ut fra fredag`() {
            Periode(12.desember(2033), 25.desember(2033)).kanFyllesUtFraOgMed() shouldBe 23.desember(2033).atTime(15, 0)
        }
    }
}
