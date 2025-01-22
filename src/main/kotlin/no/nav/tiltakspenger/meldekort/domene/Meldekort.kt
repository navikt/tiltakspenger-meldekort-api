package no.nav.tiltakspenger.meldekort.domene

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

/**
 * Vil i utgangspunktet være en periode på 14 dager, som starter på mandag og ender på søndag. Merk at den kan være utenfor vedtaksperioden.
 * Kun basert på vedtakene. Får en ny versjon dersom et nytt vedtak påvirker denne perioden.
 * TODO jah: Denne må splittes i 2 klasser [BrukersMeldekort] og [Meldeperiode]
 */
data class Meldekort(
    val id: HendelseId,
    val fnr: Fnr,
    val periode: Periode,
    val meldeperiodeKjedeId: MeldeperiodeId,
    val dager: NonEmptyList<MeldekortDag>,
    val status: MeldekortStatus,
) {
    /** Hvis en eller flere dager i denne perioden gir rett til tiltakspenger vil denne være true. */
//    val rett: MeldeperiodeStatus = if (dager.any { it.status == MeldeperiodeStatus.RETT_TIL_TILTAKSPENGER }) {
//        MeldeperiodeStatus.RETT_TIL_TILTAKSPENGER
//    } else MeldeperiodeStatus.IKKE_RETT_TIL_TILTAKSPENGER
}

fun genererDummyMeldeperiode(fnr: Fnr): Meldekort {
    val fraOgMed = LocalDate.now().let {
        val diffTilMandag = 1 - it.dayOfWeek.value
        it.plusDays(diffTilMandag.toLong())
    }

    val tilOgMed = fraOgMed.plusDays(13)

    val dager = List(14) { index ->
        MeldekortDag(
            dag = fraOgMed.plusDays(index.toLong()),
            status = MeldekortDagStatus.IKKE_REGISTRERT,
        )
    }.toNonEmptyListOrNull()!!
    val periode = Periode(fraOgMed, tilOgMed)

    return Meldekort(
        id = HendelseId.random(),
        fnr = fnr,
        periode = periode,
        meldeperiodeKjedeId = MeldeperiodeId("$fraOgMed/$tilOgMed"),
        dager = dager,
        status = MeldekortStatus.KAN_UTFYLLES,
    )
}
