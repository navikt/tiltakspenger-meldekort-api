package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import java.time.LocalDate

data class Meldekort(
    val id: HendelseId,
    val fnr: Fnr,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
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
        MeldekortDag(
            dag = fraOgMed.plusDays(index.toLong()),
            status = MeldekortDagStatus.IKKE_REGISTRERT,
            tiltakstype = TiltakstypeSomGirRett.JOBBKLUBB
        )
    }

    return Meldekort(
        id = HendelseId.random(),
        fnr = fnr,
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        meldeperiodeId = MeldeperiodeId("$fraOgMed/$tilOgMed"),
        status = MeldekortStatus.KAN_UTFYLLES,
        meldekortDager = meldekortDager,
    )
}
