package no.nav.tiltakspenger.meldekort.meldekort

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KunneIkkeLagreMeldekortFraBrukerTest {
    private val meldekortId = MeldekortId.random()
    private val kjedeId = MeldeperiodeKjedeId("2025-01-06/2025-01-19")
    private val meldeperiodeId = MeldeperiodeId.random()
    private val sisteMeldeperiodeId = MeldeperiodeId.random()
    private val kanPrøveIgjenTidspunkt = LocalDateTime.of(2025, 1, 20, 0, 0)

    @Test
    fun `FantIkkeMeldekort - kan ikke prøve igjen og logger uten personopplysninger`() {
        val feil = KunneIkkeLagreMeldekortFraBruker.FantIkkeMeldekort(meldekortId)

        feil.kanPrøveIgjen shouldBe false
        feil.loggnivå shouldBe Loggnivå.WARN
        feil.throwable shouldBe null
        feil.loggMelding shouldBe "Fant ikke meldekort med id $meldekortId for innsendende bruker. " +
            "(kanPrøveIgjen: false)"
    }

    @Test
    fun `FantIkkeMeldeperiode - kan ikke prøve igjen og logger uten personopplysninger`() {
        val feil = KunneIkkeLagreMeldekortFraBruker.FantIkkeMeldeperiode(meldekortId, kjedeId)

        feil.kanPrøveIgjen shouldBe false
        feil.loggnivå shouldBe Loggnivå.ERROR
        feil.throwable shouldBe null
        feil.loggMelding shouldBe
            "Fant ingen meldeperiode for kjedeId $kjedeId ved innsending av meldekort med id $meldekortId. " +
            "(kanPrøveIgjen: false)"
    }

    @Test
    fun `MeldekortErAlleredeMottatt - kan ikke prøve igjen og logger uten personopplysninger`() {
        val feil = KunneIkkeLagreMeldekortFraBruker.MeldekortErAlleredeMottatt(meldekortId)

        feil.kanPrøveIgjen shouldBe false
        feil.loggnivå shouldBe Loggnivå.WARN
        feil.throwable shouldBe null
        feil.loggMelding shouldBe "Meldekort med id $meldekortId er allerede mottatt og kan ikke sendes inn " +
            "på nytt. (kanPrøveIgjen: false)"
    }

    @Test
    fun `MeldekortetsMeldeperiodeErErstattet - kan ikke prøve igjen og logger uten personopplysninger`() {
        val feil = KunneIkkeLagreMeldekortFraBruker.MeldekortetsMeldeperiodeErErstattet(
            meldekortId = meldekortId,
            meldekortetsMeldeperiodeId = meldeperiodeId,
            sisteMeldeperiodeId = sisteMeldeperiodeId,
        )

        feil.kanPrøveIgjen shouldBe false
        feil.loggnivå shouldBe Loggnivå.WARN
        feil.throwable shouldBe null
        feil.loggMelding shouldBe
            "Meldekortets meldeperiode ($meldeperiodeId) er erstattet av en nyere meldeperiode " +
            "($sisteMeldeperiodeId). Meldekort med id $meldekortId kan derfor ikke sendes inn. " +
            "(kanPrøveIgjen: false)"
    }

    @Test
    fun `MeldekortErDeaktivert - kan ikke prøve igjen og logger uten personopplysninger`() {
        val feil = KunneIkkeLagreMeldekortFraBruker.MeldekortErDeaktivert(meldekortId)

        feil.kanPrøveIgjen shouldBe false
        feil.loggnivå shouldBe Loggnivå.WARN
        feil.throwable shouldBe null
        feil.loggMelding shouldBe "Meldekort med id $meldekortId er deaktivert og kan ikke sendes inn. " +
            "(kanPrøveIgjen: false)"
    }

    @Test
    fun `MeldekortErIkkeKlartTilInnsending - kan prøve igjen senere og logger uten personopplysninger`() {
        val feil = KunneIkkeLagreMeldekortFraBruker.MeldekortErIkkeKlartTilInnsending(
            meldekortId = meldekortId,
            status = MeldekortStatus.IKKE_KLAR,
            kanPrøveIgjenTidspunkt = kanPrøveIgjenTidspunkt,
        )

        feil.kanPrøveIgjen shouldBe true
        feil.kanPrøveIgjenTidspunkt shouldBe kanPrøveIgjenTidspunkt
        feil.loggnivå shouldBe Loggnivå.WARN
        feil.throwable shouldBe null
        feil.loggMelding shouldBe "Meldekort med id $meldekortId er ikke klart til innsending " +
            "(status: IKKE_KLAR). (kanPrøveIgjen: true, kanPrøveIgjenTidspunkt: $kanPrøveIgjenTidspunkt)"
    }

    @Test
    fun `UventetFeilVedLagring - kan prøve igjen og lekker ikke throwable i loggMelding`() {
        val throwable = IllegalStateException("noe sensitivt om dager")
        val feil = KunneIkkeLagreMeldekortFraBruker.UventetFeilVedLagring(meldekortId, throwable)

        feil.kanPrøveIgjen shouldBe true
        feil.loggnivå shouldBe Loggnivå.ERROR
        feil.loggMelding shouldBe "Uventet feil ved lagring av innsendt meldekort med id $meldekortId. " +
            "(kanPrøveIgjen: true)"
        feil.loggMelding shouldContain meldekortId.toString()
        feil.throwable shouldBe throwable
    }
}
