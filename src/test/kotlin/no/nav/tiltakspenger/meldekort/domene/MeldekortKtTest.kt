package no.nav.tiltakspenger.meldekort.domene

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.januar
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
}
