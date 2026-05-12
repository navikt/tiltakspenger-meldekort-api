package no.nav.tiltakspenger.meldekort.sak.infra

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.meldekort.BrukerutfyltMeldekortDTO.Status
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDag
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SaksbehandlingMeldekortDTOTest {

    @Test
    fun `toSaksbehandlingDTO - dekker alle MeldekortDagStatus-verdier`() {
        val startDato = LocalDate.of(2025, 1, 6)
        val dager = MeldekortDagStatus.entries.mapIndexed { index, status ->
            MeldekortDag(dag = startDato.plusDays(index.toLong()), status = status)
        }

        val resultat = dager.toSaksbehandlingDTO()

        resultat.values.toList() shouldContainExactlyInAnyOrder Status.entries
    }

    @Test
    fun `BrukersMeldekort_toSaksbehandlingMeldekortDTO - mapper id, sakId, meldeperiodeId og dager`() {
        val meldekort = ObjectMother.meldekort()

        val dto = meldekort.toSaksbehandlingMeldekortDTO()

        dto.id shouldBe meldekort.id.toString()
        dto.sakId shouldBe meldekort.sakId.toString()
        dto.meldeperiodeId shouldBe meldekort.meldeperiode.id.toString()
        dto.mottatt shouldBe meldekort.mottatt
        dto.dager.size shouldBe meldekort.dager.size
        dto.journalpostId shouldBe meldekort.journalpostId.toString()
    }
}
