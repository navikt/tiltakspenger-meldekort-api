package no.nav.tiltakspenger.meldekort.journalføring.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.objectmothers.ObjectMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Tester formatene veldig eksplisitt da disse ender opp direkte i brevet til bruker og vi har valgt å gjøre
 * formateringen her og ikke i tiltakspenger-pdfgen.
 *
 * DTO-ene er private i produksjonskoden, så vi asserter mot den serialiserte JSON-en (det som faktisk sendes til pdfgen) i stedet for mot DTO-typene.
 * Forventede verdier er hardkodede strings, ikke utledet fra domenet.
 *
 * Obs! Asserts går mot hardkodede verdier i tilfelle formatet blir oppdatert i tiltakspenger-libs.
 */
class BrevMeldekortDTOTest {

    @Nested
    inner class ToBrevMeldekortDTO {
        private val periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 19))
        private val meldekort = ObjectMother.meldekort(periode = periode, mottatt = LocalDateTime.of(2025, 1, 22, 12, 33))
        private val brevMeldekortDTO = objectMapper.readTree(meldekort.toDTO())

        @Test
        fun `datoer i periode blir formatert til norsk dato`() {
            brevMeldekortDTO["periode"]["fraOgMed"].asString() shouldBe "6. januar 2025"
            brevMeldekortDTO["periode"]["tilOgMed"].asString() shouldBe "19. januar 2025"
        }

        @Test
        fun `uker formateres til norsk ukenummer`() {
            brevMeldekortDTO["uke1"].asString() shouldBe "2"
            brevMeldekortDTO["uke2"].asString() shouldBe "3"
        }

        @Test
        fun `mottatt dato formateres til norsk dato og tid`() {
            brevMeldekortDTO["mottatt"].asString() shouldBe "22.01.2025 12:33"
        }

        @Test
        fun `datoer for meldekort dag vises på formatet ukedag og dato uten år`() {
            brevMeldekortDTO["dager"][0]["dag"].asString() shouldBe "Mandag 6. januar"
            brevMeldekortDTO["dager"][1]["dag"].asString() shouldBe "Tirsdag 7. januar"
            brevMeldekortDTO["dager"][2]["dag"].asString() shouldBe "Onsdag 8. januar"
        }

        @Test
        fun `statuser for dagene serialiseres til forventede strings`() {
            serialiserteStatuser() shouldBe forventedeStatusStrings
        }
    }

    @Nested
    inner class ToBrevMeldekortDTOEngelsk {
        private val periode = Periode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 19))
        private val meldekort = ObjectMother.meldekort(periode = periode, mottatt = LocalDateTime.of(2025, 1, 22, 12, 33), locale = "en")
        private val brevMeldekortDTO = objectMapper.readTree(meldekort.toDTO())

        @Test
        fun `datoer i periode blir formatert til engelsk dato`() {
            brevMeldekortDTO["periode"]["fraOgMed"].asString() shouldBe "6 January 2025"
            brevMeldekortDTO["periode"]["tilOgMed"].asString() shouldBe "19 January 2025"
        }

        @Test
        fun `uker formateres til engelsk ukenummer`() {
            brevMeldekortDTO["uke1"].asString() shouldBe "2"
            brevMeldekortDTO["uke2"].asString() shouldBe "3"
        }

        @Test
        fun `mottatt dato formateres til engelsk dato og tid`() {
            brevMeldekortDTO["mottatt"].asString() shouldBe "22.01.2025 12:33"
        }

        @Test
        fun `datoer for meldekort dag vises på formatet ukedag og dato uten år`() {
            brevMeldekortDTO["dager"][0]["dag"].asString() shouldBe "Monday 6 January"
            brevMeldekortDTO["dager"][1]["dag"].asString() shouldBe "Tuesday 7 January"
            brevMeldekortDTO["dager"][2]["dag"].asString() shouldBe "Wednesday 8 January"
        }

        @Test
        fun `statuser for dagene serialiseres til forventede strings`() {
            serialiserteStatuser() shouldBe forventedeStatusStrings
        }
    }

    /**
     *  Forventet serialisert status per dag, i rekkefølge.
     *  Hardkodede strings - ingen kobling til domene-enumen.
     */
    private val forventedeStatusStrings = listOf(
        "DELTATT_UTEN_LØNN_I_TILTAKET",
        "FRAVÆR_ANNET",
        "FRAVÆR_SYK",
        "FRAVÆR_SYKT_BARN",
        "FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU",
        "IKKE_BESVART",
        "IKKE_BESVART",
        "DELTATT_UTEN_LØNN_I_TILTAKET",
        "DELTATT_UTEN_LØNN_I_TILTAKET",
        "DELTATT_UTEN_LØNN_I_TILTAKET",
        "DELTATT_UTEN_LØNN_I_TILTAKET",
        "DELTATT_UTEN_LØNN_I_TILTAKET",
        "IKKE_BESVART",
        "IKKE_BESVART",
    )

    /**
     *  Domenestatusene som mates inn (setup).
     *  Holdes parallelt med [forventedeStatusStrings].
     */
    private val domeneStatuser = listOf(
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
        MeldekortDagStatus.FRAVÆR_ANNET,
        MeldekortDagStatus.FRAVÆR_SYK,
        MeldekortDagStatus.FRAVÆR_SYKT_BARN,
        MeldekortDagStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU,
        MeldekortDagStatus.IKKE_BESVART,
        MeldekortDagStatus.IKKE_BESVART,
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
        MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET,
        MeldekortDagStatus.IKKE_BESVART,
        MeldekortDagStatus.IKKE_BESVART,
    )

    /** Bygger et meldekort med [domeneStatuser] og returnerer de serialiserte status-stringene fra brev-JSON-en. */
    private fun serialiserteStatuser(): List<String> {
        val mandagDenneUken = nå(fixedClock).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        // Linked map for å sikre at rekkefølgen er som forventet
        val statusMap = LinkedHashMap(
            domeneStatuser.withIndex().associate { (index, status) ->
                mandagDenneUken.plusDays(index.toLong()) to status
            },
        )
        val meldekort = ObjectMother.meldekortAlleDagerGirRett(
            periode = mandagDenneUken til mandagDenneUken.plusDays(13),
            statusMap = statusMap,
        )
        val dager = objectMapper.readTree(meldekort.toDTO())["dager"]
        return forventedeStatusStrings.indices.map { index -> dager[index]["status"].asString() }
    }
}
