package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import java.time.LocalDate

data class Meldekort(
    val id: MeldekortId,
    val fnr: Fnr,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val tiltakstype: TiltakstypeSomGirRett,
    val meldeperiodeId: MeldeperiodeId,
    val meldekortDager: List<MeldekortDag>,
    val status: MeldekortStatus,
)

fun genererDummyMeldekort(fnr: Fnr): Meldekort {
    val fraOgMed = LocalDate.now().let {
        val diffTilMandag = 1 - it.dayOfWeek.value
        it.plusDays(diffTilMandag.toLong())
    }

    val tilOgMed = fraOgMed.plusDays(13)

    val meldekortDager = List(14) { index ->
        MeldekortDag(dag = fraOgMed.plusDays(index.toLong()), status = MeldekortDagStatus.IKKE_REGISTRERT)
    }

    return Meldekort(
        id = MeldekortId.random(),
        fnr = fnr,
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        meldeperiodeId = MeldeperiodeId("$fraOgMed/$tilOgMed"),
        status = MeldekortStatus.KAN_UTFYLLES,
        tiltakstype = TiltakstypeSomGirRett.JOBBKLUBB,
        meldekortDager = meldekortDager,
    )
}
