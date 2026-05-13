package no.nav.tiltakspenger.meldekort.meldekortvedtak

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.meldekort.meldekort.MeldekortDagStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class Meldekortvedtak(
    val id: VedtakId,
    val sakId: SakId,
    val opprettet: LocalDateTime,
    val erKorrigering: Boolean,
    val erAutomatiskBehandlet: Boolean,
    val meldeperiodebehandlinger: List<Meldeperiodebehandling>,
) {
    init {
        require(meldeperiodebehandlinger.isNotEmpty()) {
            "Et meldekortvedtak må ha minst én meldeperiodebehandling"
        }
    }
}

data class Meldeperiodebehandling(
    val meldeperiodeId: MeldeperiodeId,
    val meldeperiodeKjedeId: MeldeperiodeKjedeId,
    /** null dersom meldekortbehandlingen ikke er basert på et brukerinnsendt meldekort. Kan brukes for å joine mot meldekort_bruker-tabellen. */
    val brukersMeldekortId: MeldekortId?,
    val periode: Periode,
    val dager: List<MeldeperiodebehandlingDag>,
)

data class MeldeperiodebehandlingDag(
    val dato: LocalDate,
    val status: MeldekortDagStatus,
    val reduksjon: Reduksjon,
    val beløp: Int,
    val beløpBarnetillegg: Int,
)

enum class Reduksjon {
    INGEN_REDUKSJON,
    REDUKSJON,
    YTELSEN_FALLER_BORT,
}
