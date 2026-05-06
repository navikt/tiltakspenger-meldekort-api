package no.nav.tiltakspenger.meldekort.domene.varsler

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.meldekort.domene.VarselId
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PlanlagtAktiveringTest {
    private val testdato = 10.januar(2025)
    private val sakId = SakId.random()
    private val fnr = Fnr.fromString("12345678911")

    @Test
    fun `lager plan for manglende innsending med kjedens tidspunkt og ekstern varslingstidspunkt fra Varsler`() {
        val vurderingstidspunkt = testdato.atHour(10)
        val kanFyllesUtFraOgMed = testdato.minusDays(1).atHour(15)
        val kjede = kjedeSomManglerInnsending(kanFyllesUtFraOgMed = kanFyllesUtFraOgMed)

        val plan = PlanlagtAktivering.forManglendeInnsending(
            førsteKjedeSomManglerInnsending = kjede,
            antallKjederSomManglerInnsending = 2,
            varsler = Varsler(emptyList()),
            clock = fixedClockAt(vurderingstidspunkt),
        )

        plan.skalAktiveresTidspunkt shouldBe kanFyllesUtFraOgMed
        plan.skalAktiveresEksterntTidspunkt shouldBe vurderingstidspunkt
        plan.begrunnelse shouldContain "skalAktiveresTidspunkt=$kanFyllesUtFraOgMed"
        plan.begrunnelse shouldContain "skalAktiveresEksterntTidspunkt=$vurderingstidspunkt"
        plan.begrunnelse shouldContain "vurderingstidspunkt=$vurderingstidspunkt"
        plan.begrunnelse shouldContain "antallKjeder=2"
        plan.begrunnelse shouldContain "valgtKjedeId=${kjede.kjedeId}"
    }

    @Test
    fun `fabrikken bruker Varsler sin regel for utsettelse av ekstern varsling`() {
        val vurderingstidspunkt = testdato.atHour(10)
        val aktivtVarsel = Varsel.Aktiv(
            sakId = sakId,
            saksnummer = "SAK-123",
            fnr = fnr,
            varselId = VarselId.random(),
            skalAktiveresTidspunkt = testdato.atHour(9),
            skalAktiveresEksterntTidspunkt = testdato.atHour(9),
            skalAktiveresBegrunnelse = "test",
            aktiveringstidspunkt = testdato.atHour(9),
            eksternAktiveringstidspunkt = testdato.atHour(9),
            opprettet = testdato.atHour(9),
            sistEndret = testdato.atHour(9),
        )

        val plan = PlanlagtAktivering.forManglendeInnsending(
            førsteKjedeSomManglerInnsending = kjedeSomManglerInnsending(kanFyllesUtFraOgMed = vurderingstidspunkt.minusDays(1)),
            antallKjederSomManglerInnsending = 1,
            varsler = Varsler(listOf(aktivtVarsel)),
            clock = fixedClockAt(vurderingstidspunkt),
        )

        plan.skalAktiveresEksterntTidspunkt shouldBe testdato.plusDays(3).atHour(9)
    }

    @Test
    fun `beholder SkalAktiveres når pågående tidspunkt og nytt tidspunkt er i fortiden - under 1 time differanse`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atHour(9)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.plusMinutes(59)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderSkalAktiveres(
            clock = clock,
            pågåendeSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt,
        ) shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErNærNokEllerLikSkalAktiveres.left()
    }

    @Test
    fun `beholder SkalAktiveres når pågående tidspunkt og nytt tidspunkt er i fortiden - over en time differanse`() {
        val clock = fixedClockAt(testdato.atHour(12))
        val pågåendeSkalAktiveresTidspunkt = testdato.atHour(2)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.plusMinutes(243)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderSkalAktiveres(
            clock = clock,
            pågåendeSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt,
        ) shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErNærNokEllerLikSkalAktiveres.left()
    }

    @Test
    fun `beholder SkalAktiveres når pågående tidspunkt er i fortiden og nytt tidspunkt er nå`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atTime(9, 59)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.plusMinutes(1)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderSkalAktiveres(
            clock = clock,
            pågåendeSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt,
        ) shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErNærNokEllerLikSkalAktiveres.left()
    }

    @Test
    fun `beholder SkalAktiveres når nytt tidspunkt er ett minutt før grensen`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atHour(10)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.plusMinutes(59)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderSkalAktiveres(
            clock = clock,
            pågåendeSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt,
        ) shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErNærNokEllerLikSkalAktiveres.left()
    }

    @Test
    fun `beholder SkalAktiveres når nytt effektivt tidspunkt er lik grensen`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atHour(10)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.plusMinutes(60)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderSkalAktiveres(
            clock = clock,
            pågåendeSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt,
        ) shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErNærNokEllerLikSkalAktiveres.left()
    }

    @Test
    fun `beholder SkalAktiveres når pågående tidspunkt og nytt tidspunkt er i fremtiden`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atHour(11)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.plusMinutes(60)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderSkalAktiveres(
            clock = clock,
            pågåendeSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt,
        ) shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErNærNokEllerLikSkalAktiveres.left()
    }

    @Test
    fun `beholder SkalAktiveres når nytt effektivt tidspunkt er 10 minutter før pågående`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atTime(10, 20)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.minusMinutes(10)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderSkalAktiveres(
            clock = clock,
            pågåendeSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt,
        ) shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErIkkeTidligNokTilÅErstatteSkalAktiveres.left()
    }

    @Test
    fun `beholder SkalAktiveres når nytt effektivt tidspunkt er nå og pågående er 10 minutter i fremtiden`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atTime(10, 10)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.minusMinutes(10)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderSkalAktiveres(
            clock = clock,
            pågåendeSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt,
        ) shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErIkkeTidligNokTilÅErstatteSkalAktiveres.left()
    }

    @Test
    fun `beholder SkalAktiveres når nytt effektivt tidspunkt er 11 minutter før pågående og pågående er 10 minutter i fremtiden`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atTime(10, 10)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.minusMinutes(11)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderSkalAktiveres(
            clock = clock,
            pågåendeSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt,
        ) shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErIkkeTidligNokTilÅErstatteSkalAktiveres.left()
    }

    @Test
    fun `beholder SkalAktiveres når nytt effektivt tidspunkt er 10 minutter før pågående og pågående er 11 minutter i fremtiden`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atTime(10, 11)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.minusMinutes(10)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderSkalAktiveres(
            clock = clock,
            pågåendeSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt,
        ) shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErIkkeTidligNokTilÅErstatteSkalAktiveres.left()
    }

    @Test
    fun `beholder aktivt varsel når nytt tidspunkt er i fortiden`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atHour(9)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.plusMinutes(59)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderAktivtVarsel(
            clock = clock,
        ) shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErInnenforEnTimeAktivtVarsel.left()
    }

    @Test
    fun `beholder aktivt varsel når nytt tidspunkt er nå`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atTime(9, 59)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.plusMinutes(1)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderAktivtVarsel(clock) shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErInnenforEnTimeAktivtVarsel.left()
    }

    @Test
    fun `beholder aktivt varsel når nytt tidspunkt er ett minutt før grensen`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atHour(10)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.plusMinutes(59)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderAktivtVarsel(
            clock = clock,
        ) shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErInnenforEnTimeAktivtVarsel.left()
    }

    @Test
    fun `beholder aktivt varsel når nytt tidspunkt er lik grensen`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atHour(10)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.plusMinutes(60)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderAktivtVarsel(
            clock = clock,
        ) shouldBe BeholderPågåendeVarselÅrsak.PlanlagtAktiveringErInnenforEnTimeAktivtVarsel.left()
    }

    @Test
    fun `beholder ikke SkalAktiveres når nytt effektivt tidspunkt er ett minutt etter grensen`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atHour(11)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.plusMinutes(61)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderSkalAktiveres(
            clock = clock,
            pågåendeSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt,
        ) shouldBe Unit.right()
    }

    @Test
    fun `beholder ikke SkalAktiveres når nytt effektivt tidspunkt er 61 minutter etter nå`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atHour(10)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.plusMinutes(61)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderSkalAktiveres(
            clock = clock,
            pågåendeSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt,
        ) shouldBe Unit.right()
    }

    @Test
    fun `beholder ikke SkalAktiveres når nytt effektivt tidspunkt er nå og pågående er 11 minutter i fremtiden`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atTime(10, 11)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.minusMinutes(11)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderSkalAktiveres(
            clock = clock,
            pågåendeSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt,
        ) shouldBe Unit.right()
    }

    @Test
    fun `beholder ikke aktivt varsel når nytt tidspunkt er ett minutt etter grensen`() {
        val clock = fixedClockAt(testdato.atHour(10))
        val pågåendeSkalAktiveresTidspunkt = testdato.atHour(10)
        val nyttSkalAktiveresTidspunkt = pågåendeSkalAktiveresTidspunkt.plusMinutes(61)

        planlagtAktivering(nyttSkalAktiveresTidspunkt).vurderAktivtVarsel(
            clock = clock,
        ) shouldBe Unit.right()
    }

    private fun planlagtAktivering(skalAktiveresTidspunkt: LocalDateTime): PlanlagtAktivering {
        return PlanlagtAktivering(
            skalAktiveresTidspunkt = skalAktiveresTidspunkt,
            skalAktiveresEksterntTidspunkt = skalAktiveresTidspunkt,
            begrunnelse = "test",
        )
    }

    private fun kjedeSomManglerInnsending(
        kanFyllesUtFraOgMed: LocalDateTime,
        nyesteVersjon: Int = 1,
    ): KjedeSomManglerInnsending {
        return KjedeSomManglerInnsending(
            sakId = sakId,
            meldeperiodeId = MeldeperiodeId.random(),
            kjedeId = MeldeperiodeKjedeId("2025-01-06/2025-01-19"),
            nyesteVersjon = nyesteVersjon,
            kanFyllesUtFraOgMed = kanFyllesUtFraOgMed,
        )
    }
}

private fun LocalDate.atHour(time: Int): LocalDateTime = this.atTime(time, 0)
