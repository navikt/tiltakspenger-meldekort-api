package no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.infra.routes.withTestApplicationContext
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.erInnsendt
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.korrigering.MeldekortKorrigertDagDTO
import no.nav.tiltakspenger.meldekort.meldekort.infra.routes.sendInnNesteMeldekort
import no.nav.tiltakspenger.meldekort.sak.infra.routes.mottaSakRequest
import no.nav.tiltakspenger.objectmothers.ObjectMother.meldeperiodeDto
import no.nav.tiltakspenger.objectmothers.ObjectMother.tikkendeKlokke1mars2025
import org.junit.jupiter.api.Test

class KorrigerMeldekortRouteTest {

    private val periode = 6 til 19.januar(2025)

    @Test
    fun `korrigerMeldekort - korrigerer innsendt meldekort`() = runTest {
        withTestApplicationContext(clock = tikkendeKlokke1mars2025()) { tac ->
            val sak = mottaSakRequest(
                tac = tac,
                meldeperioder = listOf(meldeperiodeDto(periode = periode, opprettet = nå(tac.clock))),
            )
            val (_, innsendtMeldekort) = sendInnNesteMeldekort(tac = tac, fnr = sak.fnr.verdi)!!

            val korrigerteDager = periode.tilDager().map { dag ->
                MeldekortKorrigertDagDTO(
                    dato = dag,
                    status = if (dag.dayOfWeek.value <= 5) {
                        MeldekortDagStatus.FRAVÆR_SYK
                    } else {
                        MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER
                    },
                )
            }

            val korrigertMeldekort = korrigerMeldekortRequest(
                tac = tac,
                meldekortId = innsendtMeldekort.id.toString(),
                requestDto = korrigerteDager,
                locale = "nb",
                fnr = sak.fnr.verdi,
            )!!

            korrigertMeldekort.erInnsendt()
            korrigertMeldekort.id shouldNotBe innsendtMeldekort.id.toString()
            korrigertMeldekort.dager.size shouldBe 14
        }
    }
}
