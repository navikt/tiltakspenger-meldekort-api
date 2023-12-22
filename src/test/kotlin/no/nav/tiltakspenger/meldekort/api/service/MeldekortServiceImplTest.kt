package no.nav.tiltakspenger.meldekort.api.service

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDag
import no.nav.tiltakspenger.meldekort.api.dto.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class MeldekortServiceImplTest {
    @Test
    fun `test lagPerioder`() {
        val fom = LocalDate.of(2021, 11, 1)
        val tom = LocalDate.of(2021, 12, 19)

        val perioder = lagMeldekortPerioder(fom, tom)
        perioder shouldBe listOf(
            Periode(fra = LocalDate.of(2021, 11, 1), til = LocalDate.of(2021, 11, 14)),
            Periode(fra = LocalDate.of(2021, 11, 15), til = LocalDate.of(2021, 11, 28)),
            Periode(fra = LocalDate.of(2021, 11, 29), til = LocalDate.of(2021, 12, 12)),
            Periode(fra = LocalDate.of(2021, 12, 13), til = LocalDate.of(2021, 12, 26)),
        )
    }

    @Test
    fun `test finnMandag funksjon og lagIkkeUtfyltPeriode`() {
        val mandag = finnMandag(LocalDate.of(2021, 11, 1))
        mandag.dayOfWeek shouldBe DayOfWeek.MONDAY

        MeldekortDag.lagIkkeUtfyltPeriode(mandag, mandag.plusDays(13)) shouldBe
            listOf(
                MeldekortDag(mandag, null, MeldekortDagStatus.IKKE_UTFYLT),
                MeldekortDag(mandag.plusDays(1), null, MeldekortDagStatus.IKKE_UTFYLT),
                MeldekortDag(mandag.plusDays(2), null, MeldekortDagStatus.IKKE_UTFYLT),
                MeldekortDag(mandag.plusDays(3), null, MeldekortDagStatus.IKKE_UTFYLT),
                MeldekortDag(mandag.plusDays(4), null, MeldekortDagStatus.IKKE_UTFYLT),
                MeldekortDag(mandag.plusDays(5), null, MeldekortDagStatus.IKKE_UTFYLT),
                MeldekortDag(mandag.plusDays(6), null, MeldekortDagStatus.IKKE_UTFYLT),

                MeldekortDag(mandag.plusDays(7), null, MeldekortDagStatus.IKKE_UTFYLT),
                MeldekortDag(mandag.plusDays(8), null, MeldekortDagStatus.IKKE_UTFYLT),
                MeldekortDag(mandag.plusDays(9), null, MeldekortDagStatus.IKKE_UTFYLT),
                MeldekortDag(mandag.plusDays(10), null, MeldekortDagStatus.IKKE_UTFYLT),
                MeldekortDag(mandag.plusDays(11), null, MeldekortDagStatus.IKKE_UTFYLT),
                MeldekortDag(mandag.plusDays(12), null, MeldekortDagStatus.IKKE_UTFYLT),
                MeldekortDag(mandag.plusDays(13), null, MeldekortDagStatus.IKKE_UTFYLT),
            )
    }

    @Test
    fun `test finnSisteDag`() {
        val til = LocalDate.of(2023, 1, 31) // onsdag
        val fra = LocalDate.of(2023, 1, 4) // onsdag
        val sisteDag = finnSisteDag(finnMandag(fra), til)
        sisteDag shouldBe LocalDate.of(2023, 2, 12)
        val fra2 = LocalDate.of(2023, 1, 11) // onsdag
        val sisteDag2 = finnSisteDag(finnMandag(fra2), til)
        sisteDag2 shouldBe LocalDate.of(2023, 2, 5)
    }

    @Test
    fun `test finnSisteDagMatte`() {
        val til = LocalDate.of(2023, 1, 31) // onsdag
        val fra = LocalDate.of(2023, 1, 5) // onsdag
        val sisteDag = finnSisteDagMatte(finnMandag(fra), til)
        sisteDag shouldBe LocalDate.of(2023, 2, 12)
        val fra2 = LocalDate.of(2023, 1, 11) // onsdag
        val sisteDag2 = finnSisteDagMatte(finnMandag(fra2), til)
        sisteDag2 shouldBe LocalDate.of(2023, 2, 5)
    }
}
