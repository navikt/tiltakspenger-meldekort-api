package no.nav.tiltakspenger.meldekort.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MeldeperiodeTest {
    private val id = MeldeperiodeId.random()
    private val sakId = SakId.random()
    private val fnr = Fnr.random()
    private val opprettet = LocalDateTime.now(fixedClock)
    private val saksnummer = "saksnummer"

    @Test
    fun `2 meldeperioder er like selv om opprettet og kanFyllesUtFraOgMed er ulik`() {
        val m1 = ObjectMother.meldeperiode(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            kanFyllesUtFraOgMed = LocalDateTime.now(fixedClock),
        )
        val m2 = ObjectMother.meldeperiode(
            id = id,
            sakId = sakId,
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            kanFyllesUtFraOgMed = LocalDateTime.now(),
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
            opprettet = opprettet,
            kanFyllesUtFraOgMed = LocalDateTime.now(fixedClock),
        )
        val m2 = ObjectMother.meldeperiode(
            id = id,
            sakId = SakId.random(),
            saksnummer = saksnummer,
            fnr = fnr,
            opprettet = opprettet,
            kanFyllesUtFraOgMed = LocalDateTime.now(),
        )

        m1.erLik(m2) shouldBe false
    }
}
