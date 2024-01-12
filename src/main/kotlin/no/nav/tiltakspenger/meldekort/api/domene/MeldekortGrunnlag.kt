package no.nav.tiltakspenger.meldekort.api.domene

import no.nav.tiltakspenger.meldekort.api.felles.Periode
import java.util.*

data class MeldekortGrunnlag(
    val id: UUID,
    val vedtakId: String,
    val behandlingId: String,
    val status: Status,
    val vurderingsperiode: Periode,
    val tiltak: List<Tiltak>,
)

enum class Status {
    AKTIV,
    IKKE_AKTIV,
}

data class Tiltak(
    val id: UUID,
    val periode: Periode,
    val typeBeskrivelse: String,
    val typeKode: String,
    val antallDagerIUken: Float,
)
