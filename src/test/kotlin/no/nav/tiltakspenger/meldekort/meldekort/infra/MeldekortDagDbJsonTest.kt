package no.nav.tiltakspenger.meldekort.meldekort.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDag
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortDagDbJsonTest {
    @Test
    fun `serialiserer og deserialiserer meldekortdager til db-json`() {
        val meldekortDager = listOf(
            MeldekortDag(
                dag = LocalDate.parse("2025-01-06"),
                status = MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
            ),
            MeldekortDag(
                dag = LocalDate.parse("2025-01-07"),
                status = MeldekortDagStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU,
            ),
        )
        val forventetJson = """
            [
              {
                "dag": "2025-01-06",
                "status": "DELTATT_UTEN_LØNN_I_TILTAKET"
              },
              {
                "dag": "2025-01-07",
                "status": "FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU"
              }
            ]
        """.trimIndent()

        val serialisert = meldekortDager.tilMeldekortDagDbJson()

        objectMapper.readTree(serialisert) shouldBe objectMapper.readTree(forventetJson)
        serialisert.toMeldekortDager().toList() shouldBe meldekortDager
    }
}
