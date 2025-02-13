package no.nav.tiltakspenger.meldekort.clients.pdfgen

import BrevMeldekortDTO
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import toBrevMeldekortDTO
import java.time.LocalDate
import java.time.LocalDateTime

class BrevMeldekortDTOTest {

    /**
     * Tester formatene veldig eksplisitt da disse ender opp direkte i brevet til bruker og vi har valgt å gjøre formateringen
     * her og ikke i tiltakspenger-pdfgen.
     * Obs! Asserts går mot hardkodede verdier i tilfelle formatet blir oppdatert i tiltakspenger-libs.
     */
    @Nested
    inner class ToBrevMeldekortDTO {
        private val periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 19))
        private val meldekort = ObjectMother.meldekort(periode = periode, mottatt = LocalDateTime.of(2025, 1, 22, 12, 33))
        private val brevMeldekortDTO = deserialize<BrevMeldekortDTO>(meldekort.toBrevMeldekortDTO())

        @Test
        fun `datoer i periode blir formatert til norsk dato`() {
            brevMeldekortDTO.periode.fraOgMed shouldBe "6. januar 2025"
            brevMeldekortDTO.periode.tilOgMed shouldBe "19. januar 2025"
        }

        @Test
        fun `uker formateres til norsk ukenummer`() {
            brevMeldekortDTO.uke1 shouldBe 2
            brevMeldekortDTO.uke2 shouldBe 3
        }

        @Test
        fun `mottatt dato formateres til norsk dato og tid`() {
            brevMeldekortDTO.mottatt shouldBe "22.01.2025 12:33"
        }

        @Test
        fun `datoer for meldekort dag vises på formatet ukedag og dato uten år`() {
            brevMeldekortDTO.dager[0].dag shouldBe "Mandag 6. januar"
            brevMeldekortDTO.dager[1].dag shouldBe "Tirsdag 7. januar"
            brevMeldekortDTO.dager[2].dag shouldBe "Onsdag 8. januar"
        }
    }
}
