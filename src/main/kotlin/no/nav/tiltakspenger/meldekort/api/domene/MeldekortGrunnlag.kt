package no.nav.tiltakspenger.meldekort.api.domene

import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.meldekort.api.felles.Periode
import java.util.*

data class MeldekortGrunnlag(
    val id: UUID,
    val sakId: String,
    val vedtakId: String,
    val behandlingId: String,
    val status: Status,
    val vurderingsperiode: Periode,
    val tiltak: List<Tiltak>,
    val personopplysninger: Personopplysninger,
    val utfallsperioder: List<Utfallsperiode>,
)

data class Personopplysninger(
    val fornavn: String,
    val etternavn: String,
    val ident: String,
)

enum class Status {
    AKTIV,
    IKKE_AKTIV,
}

data class Tiltak(
    val id: UUID,
    val periode: Periode,
    val tiltakstype: TiltakstypeSomGirRett,
    val antDagerIUken: Int,
)
