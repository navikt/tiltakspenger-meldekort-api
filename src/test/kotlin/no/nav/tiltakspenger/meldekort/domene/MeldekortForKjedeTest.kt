package no.nav.tiltakspenger.meldekort.domene

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.service.KorrigerMeldekortCommand
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.temporal.ChronoUnit

class MeldekortForKjedeTest {

    @Nested
    inner class HarInnsendtMeldekort {
        @Test
        fun `true dersom minst 1 meldekort er innsendt`() {
            val clock = TikkendeKlokke()
            val meldekort1 =
                ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock)))
            val meldekort2 =
                ObjectMother.meldekort(
                    mottatt = nå(clock),
                    meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock)),
                )
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1, meldekort2))

            meldekortForKjede.harInnsendtMeldekort shouldBe true
        }

        @Test
        fun `false dersom ingen meldekort er innsendt`() {
            val clock = TikkendeKlokke()
            val meldekort1 =
                ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock)))
            val meldekort2 =
                ObjectMother.meldekort(
                    mottatt = null,
                    meldeperiode = ObjectMother.meldeperiode(versjon = 2, opprettet = nå(clock)),
                )
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1, meldekort2))

            meldekortForKjede.harInnsendtMeldekort shouldBe false
        }
    }

    @Nested
    inner class ErSisteMeldekortKlarTilInnsending {
        @Test
        fun `true dersom siste meldekort er klar til innsending`() {
            val clock = TikkendeKlokke()
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))
            val meldekort1 = ObjectMother.meldekort(
                mottatt = null,
                meldeperiode = ObjectMother.meldeperiode(periode = periode, opprettet = nå(clock)),
            )
            val meldekort2 =
                ObjectMother.meldekort(
                    mottatt = null,
                    meldeperiode = ObjectMother.meldeperiode(
                        versjon = 2,
                        periode = periode,
                        opprettet = nå(clock),
                    ),
                )
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1, meldekort2))

            meldekortForKjede.erSisteMeldekortKlarTilInnsending(
                clock = fixedClockAt(19.januar(2025).atTime(15, 0, 1)),
            ) shouldBe true
        }

        @Test
        fun `false dersom siste meldekort ikke er klar til innsending`() {
            val clock = TikkendeKlokke()
            val periode = Periode(fraOgMed = 6.januar(2025), tilOgMed = 19.januar(2025))

            val meldekort1 =
                ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode(periode = periode, opprettet = nå(clock)))
            val meldekort2 =
                ObjectMother.meldekort(meldeperiode = ObjectMother.meldeperiode(versjon = 2, periode = periode, opprettet = nå(clock)))
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1, meldekort2))

            meldekortForKjede.erSisteMeldekortKlarTilInnsending(
                clock = fixedClockAt(14.januar(2021).atTime(14, 59, 59)),
            ) shouldBe false
        }

        @Test
        fun `false dersom det ikke finnes noen meldekort i kjeden`() {
            val meldekortForKjede = MeldekortForKjede(emptyList())

            meldekortForKjede.erSisteMeldekortKlarTilInnsending(
                clock = fixedClockAt(14.januar(2021).atTime(15, 0, 1)),
            ) shouldBe false
        }
    }

    @Nested
    inner class SisteInnsendteMeldekort {
        @Test
        fun `returnerer siste meldekort som er innsendt`() {
            val clock = TikkendeKlokke()
            val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock)))
            val meldekort2 =
                ObjectMother.meldekort(mottatt = nå(clock), meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock)))
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1, meldekort2))

            meldekortForKjede.sisteInnsendteMeldekort() shouldBe meldekort2
        }

        @Test
        fun `returnerer null dersom det ikke finnes noen innsendte meldekort`() {
            val clock = TikkendeKlokke()
            val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock)))
            val meldekort2 =
                ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode(versjon = 2, opprettet = nå(clock)))
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1, meldekort2))

            meldekortForKjede.sisteInnsendteMeldekort() shouldBe null
        }
    }

    @Nested
    inner class Korrigering {
        @Test
        fun `må ha et innsendt meldekort for å korrigere`() {
            val clock = TikkendeKlokke()
            assertThrows<IllegalArgumentException> {
                val meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock))
                val meldekort = ObjectMother.meldekort(mottatt = null, meldeperiode = meldeperiode)
                MeldekortForKjede(listOf(meldekort)).korriger(
                    command = KorrigerMeldekortCommand(
                        meldekortId = meldekort.id,
                        fnr = meldekort.fnr,
                        korrigerteDager = emptyList(),
                        locale = "nb",
                    ),
                    sisteMeldeperiode = meldeperiode,
                    clock = clock,
                )
            }
        }

        @Test
        fun `meldekortet som skal korrigeres må være den siste i kjeden`() {
            val clock = TikkendeKlokke()
            val meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock))
            val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = meldeperiode)
            val meldekort2 =
                ObjectMother.meldekort(mottatt = nå(clock), meldeperiode = meldeperiode)

            val actual = MeldekortForKjede(listOf(meldekort1, meldekort2)).korriger(
                command = KorrigerMeldekortCommand(
                    meldekortId = meldekort1.id,
                    fnr = meldekort1.fnr,
                    korrigerteDager = emptyList(),
                    locale = "nb",
                ),
                sisteMeldeperiode = meldeperiode,
                clock = clock,
            )

            actual shouldBe FeilVedKorrigeringAvMeldekort.IkkeSisteMeldekortIKjeden.left()
        }

        @Test
        fun `korrigerer og fører til en oppdatering av et eksisterende meldekort til innsending`() {
            val clock = TikkendeKlokke()
            val meldeperiode1 = ObjectMother.meldeperiode(opprettet = nå(clock))
            val meldeperiode2 = ObjectMother.meldeperiode(
                sakId = meldeperiode1.sakId,
                fnr = meldeperiode1.fnr,
                periode = meldeperiode1.periode,
                saksnummer = meldeperiode1.saksnummer,
                versjon = 2,
                opprettet = nå(clock),
            )
            val mottattMeldekort1 = ObjectMother.meldekort(meldeperiode = meldeperiode1)
            val åpentMeldekort2 = ObjectMother.meldekort(mottatt = null, meldeperiode = meldeperiode2)
            val meldekortForKjede = MeldekortForKjede(listOf(mottattMeldekort1, åpentMeldekort2))

            val korrigerteDager = meldeperiode2.girRett.map { (dato, girRett) ->
                MeldekortDag(
                    dag = dato,
                    status = if (girRett) MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET else MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
                )
            }
            val actualMeldekort = meldekortForKjede.korriger(
                command = KorrigerMeldekortCommand(
                    meldekortId = mottattMeldekort1.id,
                    fnr = mottattMeldekort1.fnr,
                    korrigerteDager = korrigerteDager,
                    locale = "nb",
                ),
                sisteMeldeperiode = meldeperiode2,
                clock = fixedClockAt(meldeperiode2.periode.tilOgMed),
            )

            actualMeldekort shouldBe åpentMeldekort2.copy(
                mottatt = nå(fixedClockAt(meldeperiode2.periode.tilOgMed)),
                dager = korrigerteDager,
                korrigering = true,
                locale = "nb",
            ).right()
        }

        @Test
        fun `korrigerer og fører til et nytt meldekort`() {
            val clock = TikkendeKlokke()
            val sisteMeldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock))
            val meldekort = ObjectMother.meldekort(meldeperiode = sisteMeldeperiode)
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort))

            val actualMeldekort = meldekortForKjede.korriger(
                command = KorrigerMeldekortCommand(
                    meldekortId = meldekort.id,
                    fnr = meldekort.fnr,
                    korrigerteDager = sisteMeldeperiode.girRett.map { (dato, girRett) ->
                        MeldekortDag(
                            dag = dato,
                            status = if (girRett) MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET else MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
                        )
                    },
                    locale = "nb",
                ),
                sisteMeldeperiode = sisteMeldeperiode,
                clock = clock,
            ).getOrFail()
            actualMeldekort.id shouldNotBe meldekort.id
        }
    }

    @Nested
    inner class Inits {
        @Test
        fun `for hver versjon av en meldeperiode, kan max 1 meldekort ikke være mottatt`() {
            val clock = TikkendeKlokke()
            assertThrows<IllegalArgumentException> {
                val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock)))
                val meldekort2 = ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock)))
                MeldekortForKjede(listOf(meldekort1, meldekort2))
            }

            assertDoesNotThrow {
                val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock)))
                val meldekort2 =
                    ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode(versjon = 2, opprettet = nå(clock)))
                MeldekortForKjede(listOf(meldekort1, meldekort2))
            }

            assertDoesNotThrow {
                val meldekort1 = ObjectMother.meldekort(meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock)))
                val meldekort2 = ObjectMother.meldekort(meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock)))
                MeldekortForKjede(listOf(meldekort1, meldekort2))
            }
        }

        @Test
        fun `meldekortene må være sortert på versjon`() {
            val clock = TikkendeKlokke()
            assertThrows<IllegalArgumentException> {
                val meldekort1 = ObjectMother.meldekort(meldeperiode = ObjectMother.meldeperiode(versjon = 2, opprettet = nå(clock)))
                val meldekort2 = ObjectMother.meldekort(meldeperiode = ObjectMother.meldeperiode(versjon = 1, opprettet = nå(clock)))
                MeldekortForKjede(listOf(meldekort1, meldekort2))
            }

            assertDoesNotThrow {
                val meldekort1 = ObjectMother.meldekort(meldeperiode = ObjectMother.meldeperiode(versjon = 1, opprettet = nå(clock)))
                val meldekort2 = ObjectMother.meldekort(meldeperiode = ObjectMother.meldeperiode(versjon = 2, opprettet = nå(clock)))
                MeldekortForKjede(listOf(meldekort1, meldekort2))
            }
        }

        @Test
        fun `meldekort med samme meldeperiode versjon må være sortert på mottatt`() {
            assertThrows<IllegalArgumentException> {
                val meldekort1 =
                    ObjectMother.meldekort(
                        mottatt = nå(fixedClock),
                        meldeperiode = ObjectMother.meldeperiode(versjon = 1, opprettet = nå(fixedClock)),
                    )
                val meldekort2 = ObjectMother.meldekort(
                    mottatt = nå(fixedClock).minus(1, ChronoUnit.SECONDS),
                    meldeperiode = ObjectMother.meldeperiode(versjon = 1, opprettet = nå(fixedClock)),
                )
                MeldekortForKjede(listOf(meldekort1, meldekort2))
            }

            assertDoesNotThrow {
                val meldekort1 = ObjectMother.meldekort()
                val meldekort2 = ObjectMother.meldekort()
                MeldekortForKjede(listOf(meldekort1, meldekort2))
            }
        }
    }

    @Nested
    inner class KanMeldekortKorrigeres {
        @Test
        fun `returnerer true dersom det finnes et innsendt meldekort i kjeden`() {
            val clock = TikkendeKlokke()
            val meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock))
            val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = meldeperiode)
            val meldekort2 = ObjectMother.meldekort(mottatt = nå(clock), meldeperiode = meldeperiode)
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1, meldekort2))

            meldekortForKjede.kanMeldekortKorrigeres(meldekort2.id) shouldBe true
        }

        @Test
        fun `returnerer false dersom det ikke finnes et innsendt meldekort i kjeden`() {
            val clock = TikkendeKlokke()
            val meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock))
            val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = meldeperiode)
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1))

            meldekortForKjede.kanMeldekortKorrigeres(meldekort1.id) shouldBe false
        }

        @Test
        fun `returnerer false dersom meldekortet ikke er den siste`() {
            val clock = TikkendeKlokke()
            val meldeperiode = ObjectMother.meldeperiode(opprettet = nå(clock))
            val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = meldeperiode)
            val meldekort2 = ObjectMother.meldekort(mottatt = nå(clock), meldeperiode = meldeperiode)
            val meldekort3 = ObjectMother.meldekort(mottatt = nå(clock), meldeperiode = meldeperiode)
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1, meldekort2, meldekort3))

            meldekortForKjede.kanMeldekortKorrigeres(meldekort2.id) shouldBe false
        }
    }
}
