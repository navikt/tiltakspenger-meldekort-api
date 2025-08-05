package no.nav.tiltakspenger.service

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagFraBrukerDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatusDTO
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MeldekortServiceTest {
    private fun lagMeldekort(tac: TestApplicationContext, periode: Periode, mottatt: LocalDateTime? = null): Meldekort {
        val meldekortRepo = tac.meldekortRepo
        val meldeperiodeRepo = tac.meldeperiodeRepo

        val meldekort = ObjectMother.meldekort(
            periode = periode,
            mottatt = mottatt,
        )
        meldeperiodeRepo.lagre(meldekort.meldeperiode)
        meldekortRepo.opprett(meldekort)

        return meldekort
    }

    private val gyldigPeriode = ObjectMother.periode(LocalDate.of(2025, 1, 1))

    private fun lagMeldekortFraBrukerKommando(meldekort: Meldekort, fnr: Fnr = meldekort.fnr): LagreMeldekortFraBrukerKommando {
        return LagreMeldekortFraBrukerKommando(
            id = meldekort.id,
            mottatt = nå(fixedClock),
            fnr = fnr,
            dager = meldekort.dager.map {
                MeldekortDagFraBrukerDTO(
                    dag = it.dag,
                    status = if (meldekort.meldeperiode.girRett[it.dag] == true) MeldekortDagStatusDTO.DELTATT_UTEN_LØNN_I_TILTAKET else MeldekortDagStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER,
                )
            },
        )
    }

    @Test
    fun `Kan lagre gyldig meldekort fra bruker`() {
        val tac = TestApplicationContext()
        val meldekortRepo = tac.meldekortRepo
        val meldekortService = tac.meldekortService

        val meldekort = lagMeldekort(tac, gyldigPeriode)
        val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

        meldekortService.lagreMeldekortFraBruker(lagreKommando)

        val oppdatertMeldekort = meldekortRepo.hentForMeldekortId(meldekortId = meldekort.id, fnr = meldekort.fnr)
        val forventetMeldekort =
            meldekort.copy(mottatt = lagreKommando.mottatt, dager = lagreKommando.dager.map { it.tilMeldekortDag() })

        oppdatertMeldekort shouldBe forventetMeldekort
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker for periode som ikke er klart til innsending`() {
        val tac = TestApplicationContext()
        val meldekortService = tac.meldekortService

        val meldekort = lagMeldekort(tac, ObjectMother.periode(LocalDate.now()))
        val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

        shouldThrowWithMessage<IllegalArgumentException>("Meldekortet er ikke klart for innsending fra bruker") {
            meldekortService.lagreMeldekortFraBruker(lagreKommando)
        }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker som ikke matcher fnr på meldekortet`() {
        val tac = TestApplicationContext()
        val meldekortService = tac.meldekortService

        val meldekort = lagMeldekort(tac, ObjectMother.periode(LocalDate.of(2025, 1, 1)))
        val lagreKommando = lagMeldekortFraBrukerKommando(meldekort, fnr = Fnr.fromString("11111111111"))

        shouldThrowWithMessage<IllegalArgumentException>("Meldekort med id ${meldekort.id} finnes ikke for bruker 11111111111") {
            meldekortService.lagreMeldekortFraBruker(lagreKommando)
        }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker som allerede er mottatt`() {
        val tac = TestApplicationContext()
        val meldekortService = tac.meldekortService
        val mottatt = nå(fixedClock)

        val meldekort = lagMeldekort(tac, ObjectMother.periode(LocalDate.now()), mottatt)
        val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

        shouldThrowWithMessage<IllegalArgumentException>("Meldekort med id ${meldekort.id} er allerede mottatt ($mottatt)") {
            meldekortService.lagreMeldekortFraBruker(lagreKommando)
        }
    }
}
