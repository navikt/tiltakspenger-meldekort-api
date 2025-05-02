package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.service.SakDTO
import java.time.LocalDate
import kotlin.collections.zipWithNext

data class Sak(
    val id: SakId,
    val saksnummer: String,
    val fnr: Fnr,
    val innvilgelsesperioder: Innvilgelsesperioder,
    val arenaMeldekortStatus: ArenaMeldekortStatus,
) {

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

data class Innvilgelsesperioder(
    val value: List<Periode>,
) : List<Periode> by value {

    init {
        this.zipWithNext().forEach { (a, b) ->
            require(a.tilOgMed < b.fraOgMed) {
                "Innvilgelsesperiodene må være sortert uten overlapp - $this"
            }
        }
    }
}

fun SakDTO.tilSak(): Sak {
    return Sak(
        id = SakId.fromString(this.sakId),
        saksnummer = this.saksnummer,
        fnr = Fnr.fromString(this.fnr),
        innvilgelsesperioder = Innvilgelsesperioder(this.innvilgelsesperioder.map { it.toDomain() }),
        arenaMeldekortStatus = ArenaMeldekortStatus.UKJENT,
    )
}
