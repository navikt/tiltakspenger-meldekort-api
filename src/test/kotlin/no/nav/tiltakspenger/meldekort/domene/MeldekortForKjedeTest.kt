package no.nav.tiltakspenger.meldekort.domene

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
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
            val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode())
            val meldekort2 =
                ObjectMother.meldekort(mottatt = nå(fixedClock), meldeperiode = ObjectMother.meldeperiode())
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1, meldekort2))

            meldekortForKjede.harInnsendtMeldekort shouldBe true
        }

        @Test
        fun `false dersom ingen meldekort er innsendt`() {
            val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode())
            val meldekort2 =
                ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode(versjon = 2))
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1, meldekort2))

            meldekortForKjede.harInnsendtMeldekort shouldBe false
        }
    }

    @Nested
    inner class ErSisteMeldekortKlarTilInnsending {
        @Test
        fun `true dersom siste meldekort er klar til innsending`() {
            val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode())
            val meldekort2 =
                ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode(versjon = 2))
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1, meldekort2))

            meldekortForKjede.erSisteMeldekortKlarTilInnsending shouldBe true
        }

        @Test
        fun `false dersom siste meldekort ikke er klar til innsending`() {
            val meldekort1 = ObjectMother.meldekort(mottatt = null)
            val meldekort2 = ObjectMother.meldekort(meldeperiode = ObjectMother.meldeperiode(versjon = 2))
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1, meldekort2))

            meldekortForKjede.erSisteMeldekortKlarTilInnsending shouldBe false
        }

        @Test
        fun `false dersom det ikke finnes noen meldekort i kjeden`() {
            val meldekortForKjede = MeldekortForKjede(emptyList())

            meldekortForKjede.erSisteMeldekortKlarTilInnsending shouldBe false
        }
    }

    @Nested
    inner class SisteInnsendteMeldekort {
        @Test
        fun `returnerer siste meldekort som er innsendt`() {
            val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode())
            val meldekort2 =
                ObjectMother.meldekort(mottatt = nå(fixedClock), meldeperiode = ObjectMother.meldeperiode())
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1, meldekort2))

            meldekortForKjede.sisteInnsendteMeldekort() shouldBe meldekort2
        }

        @Test
        fun `returnerer null dersom det ikke finnes noen innsendte meldekort`() {
            val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode())
            val meldekort2 =
                ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode(versjon = 2))
            val meldekortForKjede = MeldekortForKjede(listOf(meldekort1, meldekort2))

            meldekortForKjede.sisteInnsendteMeldekort() shouldBe null
        }
    }

    @Nested
    inner class Korrigering {
        @Test
        fun `må ha et innsendt meldekort for å korrigere`() {
            assertThrows<IllegalArgumentException> {
                val meldeperiode = ObjectMother.meldeperiode()
                val meldekort = ObjectMother.meldekort(mottatt = null, meldeperiode = meldeperiode)
                MeldekortForKjede(listOf(meldekort)).korriger(
                    command = KorrigerMeldekortCommand(
                        meldekortId = meldekort.id,
                        fnr = meldekort.fnr,
                        korrigerteDager = emptyList(),
                    ),
                    sisteMeldeperiode = meldeperiode,
                    clock = fixedClock,
                )
            }
        }

        @Test
        fun `meldekortet som skal korrigeres må være den siste i kjeden`() {
            val meldeperiode = ObjectMother.meldeperiode()
            val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = meldeperiode)
            val meldekort2 =
                ObjectMother.meldekort(mottatt = nå(fixedClock), meldeperiode = meldeperiode)
            assertThrows<IllegalArgumentException> {
                MeldekortForKjede(listOf(meldekort1, meldekort2)).korriger(
                    command = KorrigerMeldekortCommand(
                        meldekortId = meldekort1.id,
                        fnr = meldekort1.fnr,
                        korrigerteDager = emptyList(),
                    ),
                    sisteMeldeperiode = meldeperiode,
                    clock = fixedClock,
                )
            }.message shouldBe "Meldekort med id ${meldekort1.id} er ikke siste meldekort i kjeden ${meldekort1.meldeperiode.kjedeId}. Kan ikke korrigere."
        }

        @Test
        fun `korrigerer og fører til en oppdatering av av eksisterende meldekort til innsending`() {
            val meldeperiode1 = ObjectMother.meldeperiode()
            val meldeperiode2 = ObjectMother.meldeperiode(
                sakId = meldeperiode1.sakId,
                fnr = meldeperiode1.fnr,
                periode = meldeperiode1.periode,
                saksnummer = meldeperiode1.saksnummer,
                versjon = 2,
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
                ),
                sisteMeldeperiode = meldeperiode2,
                clock = fixedClock,
            )

            actualMeldekort shouldBe åpentMeldekort2.copy(
                mottatt = nå(fixedClock),
                dager = korrigerteDager,
                korrigering = true,
            )
        }

        @Test
        fun `korrigerer og fører til et nytt meldekort`() {
            val sisteMeldeperiode = ObjectMother.meldeperiode()
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
                ),
                sisteMeldeperiode = sisteMeldeperiode,
                clock = fixedClock,
            )
            actualMeldekort.id shouldNotBe meldekort.id
        }
    }

    @Nested
    inner class Inits {
        @Test
        fun `for hver versjon av en meldeperiode, kan max 1 meldekort ikke være mottatt`() {
            assertThrows<IllegalArgumentException> {
                val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode())
                val meldekort2 = ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode())
                MeldekortForKjede(listOf(meldekort1, meldekort2))
            }

            assertDoesNotThrow {
                val meldekort1 = ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode())
                val meldekort2 =
                    ObjectMother.meldekort(mottatt = null, meldeperiode = ObjectMother.meldeperiode(versjon = 2))
                MeldekortForKjede(listOf(meldekort1, meldekort2))
            }

            assertDoesNotThrow {
                val meldekort1 = ObjectMother.meldekort(meldeperiode = ObjectMother.meldeperiode())
                val meldekort2 = ObjectMother.meldekort(meldeperiode = ObjectMother.meldeperiode())
                MeldekortForKjede(listOf(meldekort1, meldekort2))
            }
        }

        @Test
        fun `meldekortene må være sortert på versjon`() {
            assertThrows<IllegalArgumentException> {
                val meldekort1 = ObjectMother.meldekort(meldeperiode = ObjectMother.meldeperiode(versjon = 2))
                val meldekort2 = ObjectMother.meldekort(meldeperiode = ObjectMother.meldeperiode(versjon = 1))
                MeldekortForKjede(listOf(meldekort1, meldekort2))
            }

            assertDoesNotThrow {
                val meldekort1 = ObjectMother.meldekort(meldeperiode = ObjectMother.meldeperiode(versjon = 1))
                val meldekort2 = ObjectMother.meldekort(meldeperiode = ObjectMother.meldeperiode(versjon = 2))
                MeldekortForKjede(listOf(meldekort1, meldekort2))
            }
        }

        @Test
        fun `meldekort med samme meldeperiode versjon må være sortert på mottatt`() {
            assertThrows<IllegalArgumentException> {
                val meldekort1 =
                    ObjectMother.meldekort(
                        mottatt = nå(fixedClock),
                        meldeperiode = ObjectMother.meldeperiode(versjon = 1),
                    )
                val meldekort2 = ObjectMother.meldekort(
                    mottatt = nå(fixedClock).minus(1, ChronoUnit.SECONDS),
                    meldeperiode = ObjectMother.meldeperiode(versjon = 1),
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
}
