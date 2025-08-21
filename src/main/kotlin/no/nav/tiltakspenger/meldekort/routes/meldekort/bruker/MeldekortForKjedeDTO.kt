package no.nav.tiltakspenger.meldekort.routes.meldekort.bruker

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.meldekort.domene.MeldekortTilBrukerDTO

/**
 * Feltene er null dersom listen med meldekort er tom.
 */
data class MeldekortForKjedeDTO(
    val kjedeId: String?,
    val periode: PeriodeDTO?,
    val meldekort: List<MeldekortTilBrukerDTO>,
)
