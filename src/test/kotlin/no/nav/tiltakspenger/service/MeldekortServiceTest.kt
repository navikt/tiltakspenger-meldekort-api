package no.nav.tiltakspenger.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.TestApplicationContext
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.lagMeldekort
import no.nav.tiltakspenger.objectmothers.lagMeldekortFraBrukerKommando
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortServiceTest {
    private val gyldigPeriode = ObjectMother.periode(LocalDate.of(2025, 1, 1))

    @Test
    fun `Kan lagre gyldig meldekort fra bruker`() {
        val tac = TestApplicationContext()
        val meldekortRepo = tac.meldekortRepo
        val meldekortService = tac.meldekortService

        val meldekort = tac.lagMeldekort(ObjectMother.meldeperiode(periode = gyldigPeriode))
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

        val meldekort = tac.lagMeldekort(
            ObjectMother.meldeperiode(
                periode = ObjectMother.periode(
                    LocalDate.now(),
                ),
            ),
        )
        val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

        shouldThrowWithMessage<IllegalArgumentException>("Meldekortet er ikke klart for innsending fra bruker") {
            meldekortService.lagreMeldekortFraBruker(lagreKommando)
        }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker som ikke matcher fnr på meldekortet`() {
        val tac = TestApplicationContext()
        val meldekortService = tac.meldekortService

        val meldekort = tac.lagMeldekort(
            ObjectMother.meldeperiode(
                periode = ObjectMother.periode(LocalDate.of(2025, 1, 1)),
            ),
        )
        val lagreKommando = lagMeldekortFraBrukerKommando(meldekort, fnr = Fnr.fromString("11111111111"))

        shouldThrow<NullPointerException> { meldekortService.lagreMeldekortFraBruker(lagreKommando) }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker som allerede er mottatt`() {
        val tac = TestApplicationContext()
        val meldekortService = tac.meldekortService
        val mottatt = nå(fixedClock).plusSeconds(1)

        val meldekort = tac.lagMeldekort(
            ObjectMother.meldeperiode(
                periode = ObjectMother.periode(LocalDate.now(fixedClock)),
            ),
            mottatt,
        )
        val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

        shouldThrowWithMessage<IllegalArgumentException>("Meldekort med id ${meldekort.id} er allerede mottatt ($mottatt)") {
            meldekortService.lagreMeldekortFraBruker(lagreKommando)
        }
    }
}
