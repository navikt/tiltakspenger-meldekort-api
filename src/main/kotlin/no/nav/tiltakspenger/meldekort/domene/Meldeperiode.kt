package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import java.time.LocalDate
import java.time.LocalDateTime

data class Meldeperiode(
    val meldekortId: MeldekortId,
    val meldeperiodeInstansId: MeldekortId,
    val fnr: Fnr,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val status: MeldekortStatus,
    val innsendtTidspunkt: LocalDateTime?,
    val meldekortDager: List<MeldekortDag>,
)

fun genererDummyMeldekort(fnr: Fnr): Meldeperiode {
    val fraOgMed = LocalDate.now().let {
        val diffTilMandag = 1 - it.dayOfWeek.value
        it.plusDays(diffTilMandag.toLong())
    }

    val tilOgMed = fraOgMed.plusDays(13)

    val meldekortDager = List(14) { index ->
        MeldekortDag(dato = fraOgMed.plusDays(index.toLong()), status = MeldekortDagStatus.IkkeRegistrert)
    }

    return Meldeperiode(
        meldekortId = MeldekortId.random(),
        meldeperiodeInstansId = MeldekortId.random(),
        fnr = fnr,
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        status = MeldekortStatus.TilUtfylling,
        meldekortDager = meldekortDager,
        innsendtTidspunkt = null,
    )
}
