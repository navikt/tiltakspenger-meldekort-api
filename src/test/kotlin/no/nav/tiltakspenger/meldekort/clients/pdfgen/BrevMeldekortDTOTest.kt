package no.nav.tiltakspenger.meldekort.clients.pdfgen

import BrevMeldekortDTO
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagStatus
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import toBrevMeldekortDTO
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

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

        @Test
        fun `statuser for dagene blir riktig formatert`() {
            val mandagDenneUken = nå(fixedClock).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            // Linked map for å sikre at rekkefølgen er som forventet
            val statusMap = linkedMapOf(
                mandagDenneUken to MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                mandagDenneUken.plusDays(1) to MeldekortDagStatus.FRAVÆR_ANNET,
                mandagDenneUken.plusDays(2) to MeldekortDagStatus.FRAVÆR_SYK,
                mandagDenneUken.plusDays(3) to MeldekortDagStatus.FRAVÆR_SYKT_BARN,
                mandagDenneUken.plusDays(4) to MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV,
                mandagDenneUken.plusDays(5) to MeldekortDagStatus.IKKE_BESVART,
                mandagDenneUken.plusDays(6) to MeldekortDagStatus.IKKE_BESVART,
                mandagDenneUken.plusDays(7) to MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                mandagDenneUken.plusDays(8) to MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                mandagDenneUken.plusDays(9) to MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                mandagDenneUken.plusDays(10) to MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                mandagDenneUken.plusDays(11) to MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                mandagDenneUken.plusDays(12) to MeldekortDagStatus.IKKE_BESVART,
                mandagDenneUken.plusDays(13) to MeldekortDagStatus.IKKE_BESVART,
            )

            val meldekort = ObjectMother.meldekortAlleDagerGirRett(
                periode = mandagDenneUken til mandagDenneUken.plusDays(13),
                statusMap = statusMap,
            )
            val serialized = meldekort.toBrevMeldekortDTO()
            val deserialized = deserialize<BrevMeldekortDTO>(serialized)
            val statusList = statusMap.entries.toList().map { it.value.tilBrevMeldekortStatusDTO() }

            deserialized.dager.forEachIndexed { index, brevMeldekortDagDTO ->
                val expectedStatus = statusList[index]
                brevMeldekortDagDTO.status shouldBe expectedStatus
            }
        }
    }

    @Nested
    inner class ToBrevMeldekortDTOEngelsk {
        private val periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 19))
        private val meldekort = ObjectMother.meldekort(periode = periode, mottatt = LocalDateTime.of(2025, 1, 22, 12, 33), locale = "en")
        private val brevMeldekortDTO = deserialize<BrevMeldekortDTO>(meldekort.toBrevMeldekortDTO())

        @Test
        fun `datoer i periode blir formatert til engelsk dato`() {
            brevMeldekortDTO.periode.fraOgMed shouldBe "6. January 2025"
            brevMeldekortDTO.periode.tilOgMed shouldBe "19. January 2025"
        }

        @Test
        fun `uker formateres til engelsk ukenummer`() {
            brevMeldekortDTO.uke1 shouldBe 2
            brevMeldekortDTO.uke2 shouldBe 3
        }

        @Test
        fun `mottatt dato formateres til engelsk dato og tid`() {
            brevMeldekortDTO.mottatt shouldBe "22.01.2025 12:33"
        }

        @Test
        fun `datoer for meldekort dag vises på formatet ukedag og dato uten år`() {
            brevMeldekortDTO.dager[0].dag shouldBe "Monday 6. January"
            brevMeldekortDTO.dager[1].dag shouldBe "Tuesday 7. January"
            brevMeldekortDTO.dager[2].dag shouldBe "Wednesday 8. January"
        }

        @Test
        fun `statuser for dagene blir riktig formatert`() {
            val mandagDenneUken = nå(fixedClock).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            // Linked map for å sikre at rekkefølgen er som forventet
            val statusMap = linkedMapOf(
                mandagDenneUken to MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                mandagDenneUken.plusDays(1) to MeldekortDagStatus.FRAVÆR_ANNET,
                mandagDenneUken.plusDays(2) to MeldekortDagStatus.FRAVÆR_SYK,
                mandagDenneUken.plusDays(3) to MeldekortDagStatus.FRAVÆR_SYKT_BARN,
                mandagDenneUken.plusDays(4) to MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV,
                mandagDenneUken.plusDays(5) to MeldekortDagStatus.IKKE_BESVART,
                mandagDenneUken.plusDays(6) to MeldekortDagStatus.IKKE_BESVART,
                mandagDenneUken.plusDays(7) to MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                mandagDenneUken.plusDays(8) to MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                mandagDenneUken.plusDays(9) to MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                mandagDenneUken.plusDays(10) to MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                mandagDenneUken.plusDays(11) to MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
                mandagDenneUken.plusDays(12) to MeldekortDagStatus.IKKE_BESVART,
                mandagDenneUken.plusDays(13) to MeldekortDagStatus.IKKE_BESVART,
            )

            val meldekort = ObjectMother.meldekortAlleDagerGirRett(
                periode = mandagDenneUken til mandagDenneUken.plusDays(13),
                statusMap = statusMap,
            )
            val serialized = meldekort.toBrevMeldekortDTO()
            val deserialized = deserialize<BrevMeldekortDTO>(serialized)
            val statusList = statusMap.entries.toList().map { it.value.tilBrevMeldekortStatusDTO() }

            deserialized.dager.forEachIndexed { index, brevMeldekortDagDTO ->
                val expectedStatus = statusList[index]
                brevMeldekortDagDTO.status shouldBe expectedStatus
            }
        }
    }
}
