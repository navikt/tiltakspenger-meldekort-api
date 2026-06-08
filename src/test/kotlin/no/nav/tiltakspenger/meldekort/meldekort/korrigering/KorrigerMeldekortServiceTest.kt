package no.nav.tiltakspenger.meldekort.meldekort.korrigering

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.lagreSak
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContext
import no.nav.tiltakspenger.meldekort.meldekort.KorrigerMeldekortCommand
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import no.nav.tiltakspenger.objectmothers.lagMeldekort
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KorrigerMeldekortServiceTest {
    private val gyldigPeriode = ObjectMother.periode(LocalDate.of(2025, 1, 1))

    @Test
    fun `Korrigering flagger sak for varselvurdering`() {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val korrigerMeldekortService = tac.korrigerMeldekortService
            val meldeperiode = ObjectMother.meldeperiode(periode = gyldigPeriode, opprettet = nå(tac.clock))
            tac.lagreSak(
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

            korrigerMeldekortService.korriger(
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
