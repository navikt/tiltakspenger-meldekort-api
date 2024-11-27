package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import java.time.LocalDate
import java.time.LocalDateTime

data class Meldekort(
    val id: MeldekortId,
    val sakId: SakId,
    val fnr: Fnr,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val meldeperiodeId: MeldeperiodeId,
    val meldekortDager: List<MeldekortDag>,
    val status: String,
    val iverksattTidspunkt: LocalDateTime? = null,
)

fun genererDummyMeldekort(fnr: Fnr): Meldekort {
    val forrigeMandag = LocalDate.now().let {
        val diffTilMandag = 1 - it.dayOfWeek.value
        it.plusDays(diffTilMandag.toLong())
    }

    val meldekortDager = List(14) { index ->
        MeldekortDag(dag = forrigeMandag.plusDays(index.toLong()), status = "IKKE_REGISTRERT")
    }

    return Meldekort(
        id = MeldekortId.random(),
        sakId = SakId.random(),
        fnr = fnr,
        fraOgMed = forrigeMandag,
        tilOgMed = forrigeMandag.plusDays(13),
        meldeperiodeId = MeldeperiodeId("2021-03-01/2021-03-14"),
        status = "TilUtfylling",
        meldekortDager = meldekortDager,
    )
}
