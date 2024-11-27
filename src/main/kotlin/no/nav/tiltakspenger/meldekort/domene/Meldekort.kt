package no.nav.tiltakspenger.meldekort.domene

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Meldekort(
    // TODO Kew: Lag en type for MeldekortId
    val id: String,
    val sakId: String,
    val rammevedtakId: String,
    val fnr: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val meldekortDager: List<MeldekortDag>,
    val status: String,
    val forrigeMeldekortId: String? = null,
    val iverksattTidspunkt: LocalDateTime? = null,
)

fun genererDummyMeldekort(fnr: String): Meldekort {
    val forrigeMandag = LocalDate.now().let {
        val diffTilMandag = 1 - it.dayOfWeek.value
        it.plusDays(diffTilMandag.toLong())
    }

    val meldekortDager = List<MeldekortDag>(14) { index ->
        MeldekortDag(dag = forrigeMandag.plusDays(index.toLong()), status = "IKKE_REGISTRERT")
    }

    return Meldekort(
        id = UUID.randomUUID().toString(),
        sakId = "asdf",
        rammevedtakId = "asdf",
        fnr = fnr,
        fraOgMed = forrigeMandag,
        tilOgMed = forrigeMandag.plusDays(13),
        status = "TilUtfylling",
        meldekortDager = meldekortDager,
    )
}
