package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortDagTilBrukerDTO
import java.time.LocalDateTime

data class MeldeperiodeResponse(
    val meldeperiodeId: String,
    val kjedeId: String,
    val dager: List<MeldekortDagTilBrukerDTO>,
    val periode: PeriodeDTO,
    val mottattTidspunktSisteMeldekort: LocalDateTime,
    val maksAntallDagerForPeriode: Int,
)
