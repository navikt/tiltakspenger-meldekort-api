package no.nav.tiltakspenger.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.LagreMeldekortFraBrukerKommando
import no.nav.tiltakspenger.meldekort.domene.Meldekort
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagFraBruker
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
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
        meldekortRepo.lagre(meldekort)

        return meldekort
    }

    private fun lagMeldekortFraBrukerKommando(meldekort: Meldekort, fnr: Fnr = meldekort.fnr): LagreMeldekortFraBrukerKommando {
        return LagreMeldekortFraBrukerKommando(
            id = meldekort.id,
            mottatt = nå(),
            fnr = fnr,
            dager = meldekort.dager.map {
                MeldekortDagFraBruker(
                    dag = it.dag,
                    status = if (meldekort.meldeperiode.girRett[it.dag] == true) MeldekortDagStatus.DELTATT else MeldekortDagStatus.IKKE_REGISTRERT,
                )
            },
        )
    }

    @Test
    fun `Kan lagre gyldig meldekort fra bruker`() {
        val tac = TestApplicationContext()
        val meldekortRepo = tac.meldekortRepo
        val meldekortService = tac.meldekortService

        val meldekort = lagMeldekort(tac, ObjectMother.periode(LocalDate.of(2025, 1, 1)))
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

        shouldThrow<IllegalArgumentException> {
            meldekortService.lagreMeldekortFraBruker(lagreKommando)
        }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker som ikke matcher fnr på meldekortet`() {
        val tac = TestApplicationContext()
        val meldekortService = tac.meldekortService

        val meldekort = lagMeldekort(tac, ObjectMother.periode(LocalDate.now()))
        val lagreKommando = lagMeldekortFraBrukerKommando(meldekort, fnr = Fnr.fromString("12345678901"))

        shouldThrow<IllegalArgumentException> {
            meldekortService.lagreMeldekortFraBruker(lagreKommando)
        }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker som allerede er mottatt`() {
        val tac = TestApplicationContext()
        val meldekortService = tac.meldekortService

        val meldekort = lagMeldekort(tac, ObjectMother.periode(LocalDate.now()), nå())
        val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

        shouldThrow<IllegalArgumentException> {
            meldekortService.lagreMeldekortFraBruker(lagreKommando)
        }
    }
}
