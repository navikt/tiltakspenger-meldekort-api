package no.nav.tiltakspenger.meldekort.landingsside

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.meldekort.arena.ArenaMeldekortStatus
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class LandingssideStatusTest {

    @Test
    fun `kombinerer sak og arena-status og sorterer meldekort`() {
        val tidligst = LandingssideMeldekort(LocalDateTime.parse("2025-01-17T15:00:00"))
        val sist = LandingssideMeldekort(LocalDateTime.parse("2025-01-31T15:00:00"))
        val sak = LandingssideSak(
            fnr = Fnr.fromString("12345678910"),
            arenaMeldekortStatus = ArenaMeldekortStatus.HAR_MELDEKORT,
            harInnsendteMeldekort = false,
            meldekortTilUtfylling = listOf(sist),
        )
        val arenaStatus = LandingssideArenaStatus(
            harInnsendteMeldekort = true,
            meldekortTilUtfylling = listOf(tidligst),
        )

        sak.tilLandingssideStatus(arenaStatus, "https://www.nav.no/tiltakspenger/meldekort") shouldBe LandingssideStatus(
            harInnsendteMeldekort = true,
            meldekortTilUtfylling = listOf(tidligst, sist),
            redirectUrl = "https://www.nav.no/tiltakspenger/meldekort",
        )
    }

    @Test
    fun `arena-status kan gjøres om til landingsside-status`() {
        val meldekort = LandingssideMeldekort(LocalDateTime.parse("2025-01-17T15:00:00"))

        LandingssideArenaStatus(
            harInnsendteMeldekort = false,
            meldekortTilUtfylling = listOf(meldekort),
        ).tilLandingssideStatus("https://www.nav.no/tiltakspenger/meldekort") shouldBe LandingssideStatus(
            harInnsendteMeldekort = false,
            meldekortTilUtfylling = listOf(meldekort),
            redirectUrl = "https://www.nav.no/tiltakspenger/meldekort",
        )
    }

    @Test
    fun `landingsside-status må være sortert`() {
        val tidligst = LandingssideMeldekort(LocalDateTime.parse("2025-01-17T15:00:00"))
        val sist = LandingssideMeldekort(LocalDateTime.parse("2025-01-31T15:00:00"))

        shouldThrow<IllegalArgumentException> {
            LandingssideStatus(
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = listOf(sist, tidligst),
                redirectUrl = "https://www.nav.no/tiltakspenger/meldekort",
            )
        }.shouldHaveMessage("Meldekort til utfylling må være sortert på kanSendesFra")
    }

    @Test
    fun `landingsside-sak må være sortert`() {
        val tidligst = LandingssideMeldekort(LocalDateTime.parse("2025-01-17T15:00:00"))
        val sist = LandingssideMeldekort(LocalDateTime.parse("2025-01-31T15:00:00"))

        shouldThrow<IllegalArgumentException> {
            LandingssideSak(
                fnr = Fnr.fromString("12345678910"),
                arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = listOf(sist, tidligst),
            )
        }.shouldHaveMessage("Meldekort til utfylling må være sortert på kanSendesFra")
    }

    @Test
    fun `landingsside-arena-status må være sortert`() {
        val tidligst = LandingssideMeldekort(LocalDateTime.parse("2025-01-17T15:00:00"))
        val sist = LandingssideMeldekort(LocalDateTime.parse("2025-01-31T15:00:00"))

        shouldThrow<IllegalArgumentException> {
            LandingssideArenaStatus(
                harInnsendteMeldekort = false,
                meldekortTilUtfylling = listOf(sist, tidligst),
            )
        }.shouldHaveMessage("Meldekort til utfylling må være sortert på kanSendesFra")
    }
}
