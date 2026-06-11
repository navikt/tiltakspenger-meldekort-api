package no.nav.tiltakspenger.meldekort.meldekortvedtak.infra

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import no.nav.tiltakspenger.meldekort.meldekortvedtak.MeldeperiodebehandlingDag
import no.nav.tiltakspenger.meldekort.meldekortvedtak.Reduksjon
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldeperiodebehandlingDagDbJsonTest {

    private val periode = Periode(LocalDate.parse("2025-01-06"), LocalDate.parse("2025-01-19"))

    @Test
    fun `serialiserer og deserialiserer alle statuser og reduksjoner`() {
        val dager = listOf(
            dag(MeldekortDagStatus.DELTATT_UTEN_LØNN_I_TILTAKET, Reduksjon.INGEN_REDUKSJON, 0),
            dag(MeldekortDagStatus.DELTATT_MED_LØNN_I_TILTAKET, Reduksjon.REDUKSJON, 1),
            dag(MeldekortDagStatus.FRAVÆR_SYK, Reduksjon.YTELSEN_FALLER_BORT, 2),
            dag(MeldekortDagStatus.FRAVÆR_SYKT_BARN, Reduksjon.INGEN_REDUKSJON, 3),
            dag(MeldekortDagStatus.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU, Reduksjon.INGEN_REDUKSJON, 4),
            dag(MeldekortDagStatus.FRAVÆR_GODKJENT_AV_NAV, Reduksjon.INGEN_REDUKSJON, 5),
            dag(MeldekortDagStatus.FRAVÆR_ANNET, Reduksjon.INGEN_REDUKSJON, 6),
            dag(MeldekortDagStatus.IKKE_BESVART, Reduksjon.INGEN_REDUKSJON, 7),
            dag(MeldekortDagStatus.IKKE_TILTAKSDAG, Reduksjon.INGEN_REDUKSJON, 8),
            dag(MeldekortDagStatus.IKKE_RETT_TIL_TILTAKSPENGER, Reduksjon.INGEN_REDUKSJON, 9),
        )

        val serialisert = dager.tilDagerDbJson()
        val deserialisert = serialisert.tilMeldeperiodebehandlingDager()

        deserialisert shouldBe dager
    }

    private fun dag(status: MeldekortDagStatus, reduksjon: Reduksjon, dayOffset: Int) =
        MeldeperiodebehandlingDag(
            dato = periode.fraOgMed.plusDays(dayOffset.toLong()),
            status = status,
            reduksjon = reduksjon,
            beløp = 500,
            beløpBarnetillegg = 50,
        )
}
