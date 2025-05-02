package no.nav.tiltakspenger.meldekort.domene.bruker

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate

data class Bruker(
    val fnr: Fnr,
    val sakId: SakId,
    val saksnummer: String,
    val innvilgelsesperioder: List<Periode>,
    val arenaMeldekortStatus: ArenaMeldekortStatus,
) {
    init {
        innvilgelsesperioder.zipWithNext().forEach { (a, b) ->
            require(a.tilOgMed < b.fraOgMed) {
                "Innvilgelsesperiodene må være sortert uten overlapp - $innvilgelsesperioder"
            }
        }
    }

    fun finnGjeldendeEllerNesteInnvilgelsesperiode(): Periode? {
        val idag = LocalDate.now()
        return innvilgelsesperioder.find { it.tilOgMed <= idag }
    }
}

enum class ArenaMeldekortStatus {
    UKJENT,
    HAR_MELDEKORT,
    HAR_IKKE_MELDEKORT,
}
