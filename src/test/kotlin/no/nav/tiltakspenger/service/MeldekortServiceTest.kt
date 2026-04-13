package no.nav.tiltakspenger.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.service.KorrigerMeldekortCommand
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.objectmothers.lagMeldekort
import no.nav.tiltakspenger.objectmothers.lagMeldekortFraBrukerKommando
import no.nav.tiltakspenger.routes.withTestApplicationContext
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortServiceTest {
    private val gyldigPeriode = ObjectMother.periode(LocalDate.of(2025, 1, 1))

    @Test
    fun `Kan lagre gyldig meldekort fra bruker`() {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val meldekortRepo = tac.meldekortRepo
            val meldekortService = tac.meldekortService

            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(periode = gyldigPeriode, opprettet = nå(tac.clock)),
                locale = "en",
            )
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

            meldekortService.lagreMeldekortFraBruker(lagreKommando)

            val oppdatertMeldekort = meldekortRepo.hentForMeldekortId(meldekortId = meldekort.id, fnr = meldekort.fnr)
            val forventetMeldekort =
                meldekort.copy(
                    dager = lagreKommando.dager.map { it.tilMeldekortDag() },
                    mottatt = oppdatertMeldekort?.mottatt,
                    locale = "en",
                )

            oppdatertMeldekort shouldBe forventetMeldekort
        }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker for periode som ikke er klart til innsending`() {
        withTestApplicationContext { tac ->
            val meldekortService = tac.meldekortService

            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(
                    periode = ObjectMother.periode(
                        LocalDate.now(tac.clock),
                    ),
                    opprettet = nå(tac.clock),
                ),
            )
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

            shouldThrowWithMessage<IllegalArgumentException>("Meldekort med id ${meldekort.id} er ikke klar til innsending (status: IKKE_KLAR)") {
                meldekortService.lagreMeldekortFraBruker(lagreKommando)
            }
        }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker som ikke matcher fnr på meldekortet`() {
        withTestApplicationContext { tac ->
            val meldekortService = tac.meldekortService

            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(
                    periode = ObjectMother.periode(LocalDate.of(2025, 1, 1)),
                    opprettet = nå(tac.clock),
                ),
            )
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort, fnr = Fnr.fromString("11111111111"))

            shouldThrow<NullPointerException> { meldekortService.lagreMeldekortFraBruker(lagreKommando) }
        }
    }

    @Test
    fun `Kan ikke lagre meldekort fra bruker som allerede er mottatt`() {
        withTestApplicationContext { tac ->
            val meldekortService = tac.meldekortService
            val mottatt = nå(tac.clock).plusSeconds(1)

            val meldekort = tac.lagMeldekort(
                ObjectMother.meldeperiode(
                    periode = ObjectMother.periode(LocalDate.now(fixedClock)),
                    opprettet = nå(tac.clock),
                ),
                mottatt,
            )
            val lagreKommando = lagMeldekortFraBrukerKommando(meldekort)

            shouldThrowWithMessage<IllegalArgumentException>("Meldekort med id ${meldekort.id} er allerede mottatt ($mottatt)") {
                meldekortService.lagreMeldekortFraBruker(lagreKommando)
            }
        }
    }

    @Test
    fun `Korrigering flagger sak for varselvurdering`() {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val meldekortService = tac.meldekortService
            val meldeperiode = ObjectMother.meldeperiode(periode = gyldigPeriode, opprettet = nå(tac.clock))
            tac.sakRepo.lagre(
                ObjectMother.sak(
                    id = meldeperiode.sakId,
                    saksnummer = meldeperiode.saksnummer,
                    fnr = meldeperiode.fnr,
                    meldeperioder = listOf(meldeperiode),
                ),
            )
            val innsendtMeldekort = tac.lagMeldekort(meldeperiode = meldeperiode, mottatt = nå(tac.clock))

            val korrigerteDager = innsendtMeldekort.dager.mapIndexed { index, dag ->
                if (index == 0) {
                    dag.copy(status = MeldekortDagStatus.FRAVÆR_SYK)
                } else {
                    dag
                }
            }

            meldekortService.korriger(
                KorrigerMeldekortCommand(
                    meldekortId = innsendtMeldekort.id,
                    fnr = innsendtMeldekort.fnr,
                    korrigerteDager = korrigerteDager,
                    locale = "nb",
                ),
            )

            tac.sakVarselRepo.hentSakerSomSkalVurdereVarsel().map { it.sakId } shouldBe listOf(meldeperiode.sakId)
        }
    }
}
