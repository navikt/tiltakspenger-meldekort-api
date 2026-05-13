package no.nav.tiltakspenger.meldekort.sak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SakTest {

    @Test
    fun `init - kaster når meldeperioder overlapper og har samme versjon`() {
        val periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 19))
        val meldeperiode1 = ObjectMother.meldeperiode(periode = periode, versjon = 1, opprettet = nå(fixedClock))
        val meldeperiode2 = ObjectMother.meldeperiode(periode = periode, versjon = 1, opprettet = nå(fixedClock))

        shouldThrow<IllegalArgumentException> {
            ObjectMother.sak(meldeperioder = listOf(meldeperiode1, meldeperiode2))
        }.message.shouldContain("Meldeperioder må være sortert etter periode og versjon")
    }

    @Test
    fun `init - aksepterer overlappende meldeperioder med stigende versjon`() {
        val periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 19))
        val meldeperiode1 = ObjectMother.meldeperiode(periode = periode, versjon = 1, opprettet = nå(fixedClock))
        val meldeperiode2 = ObjectMother.meldeperiode(periode = periode, versjon = 2, opprettet = nå(fixedClock))

        val sak = ObjectMother.sak(meldeperioder = listOf(meldeperiode1, meldeperiode2))

        sak.meldeperioder.size shouldBe 2
    }
}
