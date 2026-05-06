package no.nav.tiltakspenger.meldekort.domene

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class MeldekortKtTest {
    @Test
    fun `mapper fra localdate+boolean til meldekortDager`() {
        mapOf(
            30.desember(2024) to true,
            31.desember(2024) to true,
            1.januar(2025) to true,
            2.januar(2025) to true,
            3.januar(2025) to true,
            4.januar(2025) to false,
            5.januar(2025) to false,
            6.januar(2025) to true,
            7.januar(2025) to true,
            8.januar(2025) to true,
            9.januar(2025) to true,
            10.januar(2025) to true,
            11.januar(2025) to false,
            12.januar(2025) to false,
        ).tilMeldekortDager() shouldBe listOf(
            MeldekortDag(dag = 30.desember(2024), status = MeldekortDagStatus.IKKE_BESVART),
            MeldekortDag(dag = 31.desember(2024), status = MeldekortDagStatus.IKKE_BESVART),
            MeldekortDag(dag = 1.januar(2025), status = MeldekortDagStatus.IKKE_BESVART),
            MeldekortDag(dag = 2.januar(2025), status = MeldekortDagStatus.IKKE_BESVART),
            MeldekortDag(dag = 3.januar(2025), status = MeldekortDagStatus.IKKE_BESVART),
            MeldekortDag(dag = 4.januar(2025), status = MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER),
            MeldekortDag(dag = 5.januar(2025), status = MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER),
            MeldekortDag(dag = 6.januar(2025), status = MeldekortDagStatus.IKKE_BESVART),
            MeldekortDag(dag = 7.januar(2025), status = MeldekortDagStatus.IKKE_BESVART),
            MeldekortDag(dag = 8.januar(2025), status = MeldekortDagStatus.IKKE_BESVART),
            MeldekortDag(dag = 9.januar(2025), status = MeldekortDagStatus.IKKE_BESVART),
            MeldekortDag(dag = 10.januar(2025), status = MeldekortDagStatus.IKKE_BESVART),
            MeldekortDag(dag = 11.januar(2025), status = MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER),
            MeldekortDag(dag = 12.januar(2025), status = MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER),
        )
    }

    @Test
    fun `tilOppdatertMeldekort returnerer null når ingen dager gir rett`() {
        val eksisterendeMeldekort = ObjectMother.meldekort(mottatt = null)
        val oppdatertMeldeperiode = eksisterendeMeldekort.meldeperiode.copy(
            id = no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId.random(),
            versjon = eksisterendeMeldekort.meldeperiode.versjon + 1,
            opprettet = nå(fixedClock),
            girRett = eksisterendeMeldekort.meldeperiode.periode.tilDager().associateWith { false },
            maksAntallDagerForPeriode = 0,
        )

        oppdatertMeldeperiode.tilOppdatertMeldekort(eksisterendeMeldekort) shouldBe null
    }

    @Test
    fun `tilOppdatertMeldekort returnerer null når forrige meldekort er innsendt`() {
        val innsendtMeldekort = ObjectMother.meldekort(mottatt = nå(fixedClock))
        val oppdatertMeldeperiode = innsendtMeldekort.meldeperiode.copy(
            id = no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId.random(),
            versjon = innsendtMeldekort.meldeperiode.versjon + 1,
            opprettet = nå(fixedClock),
        )

        oppdatertMeldeperiode.tilOppdatertMeldekort(innsendtMeldekort) shouldBe null
    }

    @Test
    fun `tilOppdatertMeldekort gjenbruker og nullstiller for aktivt meldekort`() {
        val periode = ObjectMother.periode(tilSisteSøndagEtter = 12.januar(2025))
        val eksisterendeMeldekort = ObjectMother.meldekort(
            mottatt = null,
            locale = "nn",
            statusMap = mapOf(
                4.januar(2025) to MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER,
                6.januar(2025) to MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
            ),
            meldeperiode = ObjectMother.meldeperiode(
                periode = periode,
                opprettet = nå(fixedClock),
                girRett = mapOf(
                    30.desember(2024) to true,
                    31.desember(2024) to true,
                    1.januar(2025) to true,
                    2.januar(2025) to true,
                    3.januar(2025) to true,
                    4.januar(2025) to false,
                    5.januar(2025) to false,
                    6.januar(2025) to true,
                    7.januar(2025) to true,
                    8.januar(2025) to true,
                    9.januar(2025) to true,
                    10.januar(2025) to true,
                    11.januar(2025) to false,
                    12.januar(2025) to false,
                ),
            ),
        )
        val oppdatertMeldeperiode = eksisterendeMeldekort.meldeperiode.copy(
            id = no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId.random(),
            versjon = eksisterendeMeldekort.meldeperiode.versjon + 1,
            opprettet = nå(fixedClock),
            girRett = eksisterendeMeldekort.meldeperiode.girRett
                .toMutableMap()
                .also {
                    it[4.januar(2025)] = true
                    it[6.januar(2025)] = false
                }
                .toMap(),
        )

        val oppdatertMeldekort = oppdatertMeldeperiode.tilOppdatertMeldekort(eksisterendeMeldekort)!!

        oppdatertMeldekort.id shouldNotBe eksisterendeMeldekort.id
        oppdatertMeldekort.meldeperiode shouldBe oppdatertMeldeperiode
        oppdatertMeldekort.mottatt shouldBe null
        oppdatertMeldekort.deaktivert shouldBe null
        oppdatertMeldekort.locale shouldBe null
        oppdatertMeldekort.dager.first { it.dag == 4.januar(2025) }.status shouldBe MeldekortDagStatus.IKKE_BESVART
        oppdatertMeldekort.dager.first { it.dag == 6.januar(2025) }.status shouldBe MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
    }
}
